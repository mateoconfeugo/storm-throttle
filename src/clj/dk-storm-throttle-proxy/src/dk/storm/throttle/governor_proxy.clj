(ns dk.storm.throttle.governor-proxy
  (:use clojure.contrib.server-socket)
  (:require [redis.core :as redis])    
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)
  (:use [cheshire.core])
  (:import [dk.storm.throttle.api Budget-Item])
  (:import [java.io BufferedReader InputStreamReader OutputStreamWriter]))

(defn to-tuple-map
  [record]
  (let [modified-map {:feed-id (record "feed-id") :level-id (record "level-id") :level (record "level") :allotment (record "allotment") :current-count (record "current-count")}]
    (map->Budget-Item modified-map)))


(defn gen-budget-key [budget-item]
  (str (:feed-id budget-item ) "-" (:level budget-item) "-" ( :level-id budget-item)))

(def budget-item-1 (map->Budget-Item {:feed-id 8087 :level-id 3 :level "source" :current-count 100 :allotment 10000}))

(defn to-perl-json
  "transform the budget record into a json string that perl can deserialize into a budget object"
  [r]
  (let [raw  { :feed_id (:feed-id r) :level_id (:level-id r) :level (:level r) :allotment (:allotment r) :current_count (:current-count r)}
        tmp (dissoc raw :feed-id :level-id :current-count)]
    (generate-string tmp)))
        

(defn insert-budget-item
  "Puts the record in to the cache"
  [input spec]
  (let [damap (parse-string input)
        input (to-tuple-map damap)
        json (to-perl-json input)
        token (gen-budget-key input)]
    (redis/with-server spec
      (redis/set token json))
    (flush)))

(defn -main
  "simple multiplexing server that takes a json string of a budget item and inserts it into the cache"
  [port]
  (let [cfg (-> (config) :governor)
        settings (:budget-cache-path cfg)
        cache-host "10.0.147.102"
        cache-port 6379
        redis-server-spec {:host cache-host :db 10 :port cache-port}]        
    (letfn [(process-budgets [in out]
              (binding [*in* (BufferedReader. (InputStreamReader. in))
                        *out* (OutputStreamWriter. out)]
                (loop []
                  (let [record (read-line)]
;;                    (println  record)
                    (insert-budget-item record redis-server-spec)
                  (recur)))))]
      (create-server port process-budgets))))

                

