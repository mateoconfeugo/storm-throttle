(ns dk.storm.throttle.test.score
    (:use overtone.at-at)  
    (:use [dk.storm.throttle.config])
    (:require [redis.core :as redis])
;;  (:use [clojure.java.jdbc])  
    (:use [dk.storm.throttle.api])
      (:use [cheshire.core])
  (:use [dk.storm.throttle.score]) 
  (:import [dk.storm.throttle.api Budget-Item Usage-Summary Node-Usage-Summary Cluster-Usage-Summary])
  (:use [clojure.test])
  (:require [clojure.java.jdbc :as sql]))





(def cfg  (-> (config) :scorekeeper))
(def db-cfgs {:db-host (-> cfg :throttle-database :host)
              :db-port (-> cfg :throttle-database :port)
              :db-user (-> cfg :throttle-database :db-user)
              :db-password (-> cfg :throttle-database :db-password)
              :db-name (-> cfg :throttle-database :db-name)})


(defn test-callback-fn [sk]
(do  (println "scorekeeper callback called but for what")))


(def settings (assoc db-cfgs :timer-cb test-callback-fn :clock-period 100 :delay 1000 :cache-host "10.0.147.102" :cache-db-index (:cache-db-index cfg) :cache-port 6379))

(def throttle-db {:classname "com.mysql.jdbc.Driver"
                :subprotocol "mysql"
                :subname (str "//" (:db-host db-cfgs) ":" (:db-port db-cfgs) "/" (:db-name db-cfgs))
                :user (:db-user db-cfgs)
                :password (:db-password db-cfgs)})


(def summary-1 (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 105 :node 21}))
(def summary-2 (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 200 :node 21}))
(def summary-3 (map->Usage-Summary {:feed-id 8002 :level-id 1 :level "source" :count 300 :node 22}))
(def summary-4 (map->Usage-Summary {:feed-id 8003 :level-id 1 :level "source" :count 400 :node 23}))
(def summary-5 (map->Usage-Summary {:feed-id 8004 :level-id 1 :level "source" :count 500 :node 23}))
(def summary-6 (map->Usage-Summary {:feed-id 8004 :level-id 2 :level "source" :count 600 :node 24}))

(def test-summaries [summary-1 summary-3 summary-4 summary-5 summary-6])


(def fs-1 {:feed_id 8087 :source_id 1  :current_count 105 :allocated 30000})
(def fs-2 {:feed_id 8087 :source_id 2  :current_count 200 :allocated 30000})
(def fs-3 {:feed_id 8002 :source_id 1  :current_count 300 :allocated 30000})
(def fs-4 {:feed_id 8003 :source_id 1  :current_count 400 :allocated 30000})
(def fs-5 {:feed_id 8004 :source_id 1  :current_count 500 :allocated 30000})
(def fs-6 {:feed_id 8004 :source_id 2  :current_count 600 :allocated 30000})
(def test-tuples [fs-1 fs-2 fs-3 fs-4 fs-5 fs-6])

(def test-active-nodes ["mp21" "mp22" "mp23" "mp24"])
(def test-sk (new-scorekeeper settings))

(defn populate-usage-summary-cache []
  (redis/with-server {:host "10.0.147.102" :port 6379 :db 10}
    (doseq [sum test-summaries]
      (let [k (gen-scoreboard-key sum)]
        (redis/set k (generate-string (usage-summary-to-db-data sum)))))))

(defn delete-usage-summary-cache []
  (redis/with-server {:host "10.0.147.102" :port 6379 :db 10}
    (redis/flushdb)))

(defn delete-feed-source-table []
  (sql/with-connection throttle-db
    (sql/do-commands "DELETE FROM feed_source")))


;; (sql/with-connection throttle-db (sql/update-or-insert-values :feed_source ["feed_id=? and source_id=?" (:feed_id fs-1)  (:source_id fs-1) ] fs-1))
 
(defn populate-feed-source-table [summaries]
  (let [table "feed_source"
        usr (first summaries)]
    (if (map? usr)
      (do
        (sql/with-connection throttle-db (sql/update-or-insert-values :feed_source ["feed_id=? and source_id=?" (:feed_id usr)  (:source_id usr) ] usr))
        (recur (populate-feed-source-table (rest summaries)))))))

(defn show-feed-source-table []
  (sql/with-connection throttle-db
    (sql/with-query-results rs ["select * from feed_source"]
      (dorun (map #(println (:feed_id %) (:source_id %)) rs)))))

(deftest new-scorekeeper-test
  (let [sk (new-scorekeeper db-cfgs)]))

(deftest update-total-query-count-test
  
  (let [sk (new-scorekeeper settings)
        test-sql (str "select * from feed_source  feed_id=" (:feed-id summary-1) " and source_id=" (:level-id summary-1) " limit 1")]
    (do
      (delete-feed-source-table)
      (def  test-result (update-total-query-count sk summary-1))
      (sql/with-connection
        (sql/with-query-results rs [test-sql]
          (is (= (:feed-id summary-1) (:feed-id (first rs)))))))))

(deftest update-scoreboard-test
  (let [sk (new-scorekeeper settings)
        test-result (update-scoreboard sk summary-1)]
    (is (= test-result summary-1))))

(comment
(deftest process-usage-summary-test
  (let [sk (new-scorekeeper settings)]
    (is (= summary-1 (process-usage-summary sk summary-1)))))

(deftest active-nodes-test
  (let [sk (new-scorekeeper settings)
        test-result (active-nodes sk)]
    (is (= test-result test-active-nodes))))
)

                                        ;(run-tests 'dk.storm.throttle.test.score)
