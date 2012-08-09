(ns dk.storm.throttle.govern
  (:use dk.storm.throttle.api)
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.governor-proxy-client)
  (:import [dk.storm.throttle.api Budget-Item])
  (:require [cupboard.core :as cb])
  (:use cupboard.bdb.je)
  (:require [redis.core :as redis])    
  (:use [cheshire.core])
  (:use lamina.core)
  (:use aleph.tcp)
  (:use gloss.core)
  (:use [cupboard.utils]))

(defprotocol Govern
  "End point of system that delivers updated budget records to the throttled clients cache"
  (update-budget-item [this record]
    "inserts  budget items into memory")
  (get-budget-item [this cache-key]
    "get the budget item")
  (send-budget-item [this json]
    "Send via tcp the jsonified budget-item record to query server"))

(defn new-governor
  [budget-cache-path port nodes dbindex]
  (let [redis-server-spec {:host budget-cache-path :db dbindex :port port}]

    (reify Govern
      (get-budget-item [this cache-key]
        (cupboard.bdb.je/db-get db cache-key))

      (send-budget-item [this record]
        (publish-budget record))

      (update-budget-item [this record]
        (let [k (str (:feed-id record) "-" (:level record) "-" (:level-id record))
              v (generate-string record)] 
          (cupboard.bdb.je/db-put db k v))))))
                                    
