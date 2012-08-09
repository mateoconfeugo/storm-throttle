(ns dk.storm.throttle.score
  (:use clojure.contrib.logging)
;;  (:use [clojure.java.jdbc])
  (:use dk.storm.throttle.api)
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.sampling)
  (:use dk.storm.throttle.budget)
  (:require [redis.core :as redis])  
  (:use [cheshire.core])
  (:require tron)
  (:refer-clojure :exclude (resultset-seq))
  (:require [clojure.java.jdbc :as sql]))

(defn retrieve-budget-item-from-cache [items spec]
  (redis/with-server  spec
    (doall (map #(parse-string (redis/get % )) items))))
      
(defn parse-int [s]
    (Integer. (re-find #"[0-9]*" s)))

(defn determine-table
  "Figure out what table to operate on"
  [record]
  (let [level (:level record)]
    (str "feed_" level)))

(defn to-tuple-map
  [record]
  (let [tuple-keys (map (fn [k] (keyword (clojure.string/replace (clojure.string/replace k "-" "_") ":" ""))) (keys record))]
    (zipmap tuple-keys (vals record))))

(defn usage-summary-to-db-data  [usage-summary]
  (assoc (dissoc (to-tuple-map usage-summary) :level :level_id :count :node) :source_id (:level-id usage-summary) :current_count (:count usage-summary)))

;; pull out into general purpose lib
(defn gen-scoreboard-key [usage-summary]
  (str (:feed-id usage-summary) "-" (:level usage-summary) "-" (:level-id usage-summary)))

(defn generate-budget-item-record
  "Create a budget item record from a database tuple from the feed level table"
  [tuple]
  (let [fid (:feed_id tuple)
        lid (:source_id tuple)
        level "source"
        allocated (:allocated tuple)
        count (:current_count tuple)]
    (map->Budget-Item {:feed-id fid :level-id lid :level level :allotment allocated :current-count count})))

(defn retrieve-record-from-cache
  "get a record from the cache"
  [key spec]
  (redis/with-server spec 
    (let [existing-record (parse-string (redis/get key))]
      (into {}
            (for [[k v] existing-record]
              [(keyword k) v])))))

(defprotocol Score
    "Control calculation and accounting aspects of the throttle throttling system"
    (show-settings [this]
      "show the configuration settings")
    (process-usage-summary [this usage-summary]
      "handle incoming usage-summary tuples the top of the real time aggregation of the counts across the cluster")
    (apply-weights [this node-usage-detail-record]
      "Based on some algorithms like adjusting for delivery node outages the adjusted nodes part of the remaining query balance")
    (active-nodes [this]
      "Based on the the summaries coming in over the previous sample period what nodes are reporting and therefore active" )
    (update-totals [this usage-summary]
      "Running total of the total requests being served per node")
    (update-scoreboard [this usage-summary]
      "Updates the cache that holds all the summary usage records of feed level combinations that have changed")
    (update-total-query-count [this usage-summary]
      "Advance and store the current amount of query budget the whole system has used for this feed-id level level-id tuple")
    (start-clock [this]
      "Starts the sample period clock that drives the scoring updates")
    (stop-clock [this]
      "Starts the sample period clock that drives the scoring updates")
    (get-budget-items [this]  
      "Get the budget-items from the centrialized cache"))


;; (redis/with-server redis-server-spec (doseq [k (redis/keys "*")] (redis/get k))))

                                        ; CONSTUCTOR
(defn new-scorekeeper
  [{:keys[db-host db-port db-name db-user db-password cache-host timer-cb clock-period delay cache-port cache-db-index] :as settings}]  
  (let [throttle-db {:classname "com.mysql.jdbc.Driver"
                   :subprotocol "mysql"
                   :subname (str "//" db-host ":" db-port "/" db-name)
                   :user db-user
                   :password db-password}
        redis-server-spec {:host cache-host :db 10 :port cache-port}
        node-summary-totals (ref {})
        budget (new-budget throttle-db)]

    (reify Score
      (show-settings [this]
        (println settings))

      (process-usage-summary
        [this usage-summary-record]
        (let [budget-item (update-total-query-count this usage-summary-record)]
          (do
            (update-scoreboard this budget-item)
            (update-totals this usage-summary-record))))

      (get-budget-items [this]
        (redis/with-server redis-server-spec
          (let [items (retrieve-budget-item-from-cache (redis/keys "*") redis-server-spec)
                trans-fn (fn [k] (map->Budget-Item {:feed-id (k "feed-id") :level-id (k "level-id") :level (k "level") :allotment (k "allotment") :current-count (k "current-count")}))]
                (map #(trans-fn %) items))))

      ; fix the db 8 thing
      (active-nodes [this]
        (let [spec (assoc redis-server-spec :db 8)]
          (redis/with-server spec
            (let [raw (remove (fn [s] (clojure.string/blank? s)) (redis/keys "*"))
                  num (map (fn [x](parse-int x)) raw)]
              (into [] num)))))
      
      (update-totals [this usage-summary-record]
        (let [spec (assoc redis-server-spec :db 8)]
          (redis/with-server  spec
            (let [node-id (:node usage-summary-record)
                  summary-count (:count usage-summary-record)]
              (if (redis/exists node-id)
                (redis/incrby  node-id  summary-count)
                (redis/set node-id  summary-count))))))

      (update-scoreboard [this budget-item]
        (redis/with-server redis-server-spec 
          (let [lookup-key (gen-scoreboard-key budget-item)]
            (redis/set lookup-key (generate-string budget-item)))))

      (update-total-query-count [this usage-summary]
        (sql/with-connection throttle-db
          (let [table (determine-table usage-summary)
                tuple (usage-summary-to-db-data usage-summary)
                fid (:feed_id tuple)
                src-id (:source_id tuple)
                sql-st (str "select * from feed_source where feed_id=" fid " and  source_id= " src-id)
                existing-record (sql/with-query-results rs [sql-st] (generate-budget-item-record  (first rs)))]

            (if (map? existing-record)
              (let [summary-count (:count usage-summary)
                    existing-count (:current-count existing-record)
                    updated-count (+ existing-count summary-count)
                    updated-record (assoc tuple :current_count updated-count)]
                (do
                  (sql/update-or-insert-values :feed_source ["feed_id=? and source_id=?" fid src-id ] updated-record)
                  (sql/with-query-results rs [sql-st] (generate-budget-item-record  (first rs)))))
                (do
                  (sql/update-or-insert-values :feed_source ["feed_id=? and source_id=?" fid src-id ] tuple)
                  (sql/with-query-results rs [sql-st] (generate-budget-item-record  (first rs))))))))
                  
      (apply-weights [this node-usage-detail-record]
        (println "applying the weights"))

      (start-clock [this]
        (warn "Starting SCOREKEEPER interval sampling clock")
        (tron/periodically :score-period  #(timer-cb this) clock-period))

      (stop-clock [this]
         (tron/cancel :score-period)))))


(comment

(defn update-total-select-count
  "Advance and store the current amount of query budget this node has used"
  [{:keys [current-time-period level-type level-id feed-id]}]
  (let []))

(defn apply-throttle
  ""
  [{:keys [:feed :cache]}]
  (let []))

(defn import-query-scoreboard 
  "Loads up the initially data when the throttling is loaded/activated"
  [scoreboard-path]
  (System/setProperty "entityExpansionLimit" (str Integer/MAX_VALUE))
  (let [writer-agent (agent [])]
    (doseq [new-entry (etexts (scoreboard scoreboard-path lazy-xml/parse-trim))]
      (send writer-agent))))

)
