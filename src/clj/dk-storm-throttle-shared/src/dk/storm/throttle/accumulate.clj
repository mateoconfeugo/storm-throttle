(ns dk.storm.throttle.accumulate
  (:use [cheshire.core])  
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)
  (:use overtone.at-at)
  (:require tron)
  (:require [redis.core :as redis])  
  (:import [dk.storm.throttle.api Usage-Detail Usage-Summary Node-Usage-Summary]))

                                        ; FUNCTIONS
(defn gen-usage-count-key
  "for the lookup key to query the cache"
  [udr]
  (str (:feed-id udr) "-" (:level udr) "-" (:level-id udr)))

                                        ; PROTOCOL

(defprotocol Accumulate
  "Handle the totaling of query requests based on level-id/level-type/node-id"
  (summarize [this]
    "Generate the total count across active nodes returning a sequence usage-summary records")
  (update-count [this udr]
    "update the count cache once per sample period")
  (flush-usage [this]
    "flushes the usage cache")
  (generate-usage-summary [this token]
    "create a usage summary record based of the key and count")
  (start-clock [this]
    "start periodic timer  with correct frequency")
  (stop-clock [this]
    "start periodic timer with correct frequency"))

                                        ;REIEFED PROTOCOL COMPONENTS
(defn new-accumulator
  [cache-host cache-port cache-db-index clock-period delay timer-cb]
  (let [my-pool (mk-pool)
        usage-spec {:host cache-host :db cache-db-index :port cache-port}]

        (reify Accumulate

          (summarize [this]
            (redis/with-server usage-spec
              (let [items (redis/keys "*")]
                (doall (map #(generate-usage-summary this %)  items)))))

          (flush-usage [this]
            (redis/with-server usage-spec
              (redis/flushdb)))

          (update-count [this udr]
            (redis/with-server usage-spec
              (let [lookup-key (gen-usage-count-key udr)
                    count (:count udr)]
                (if (redis/exists lookup-key)
                  (redis/incr lookup-key)
                  (redis/set lookup-key 1)))))

          (generate-usage-summary
            [this token]
            (redis/with-server usage-spec
              (let [count (redis/get token)
                    tmp (clojure.string/split token #"-")
                    fid (get tmp 0)
                    level (get tmp 1)
                    level-id (get tmp 2)]
                (map->Usage-Summary {:feed-id fid :level level :level-id level-id :count count}))))
                    
          (start-clock [this]
            ;;(every clock-period #(timer-cb this) my-pool :initial-delay delay))
            (tron/periodically :summary-period #(timer-cb this) clock-period))
          
          (stop-clock [this]
            ;;(stop-and-reset-pool! my-pool :strategy :kill)))))
            (tron/cancel :summary-period)))))
  


