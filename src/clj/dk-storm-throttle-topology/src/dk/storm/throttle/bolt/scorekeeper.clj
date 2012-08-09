(comment :Description
         This bolt handlings input streams from accumulator bolts and once every sample period it generates a new
         updated budget emitted to the governor bolts. The score component's timer callback is connected to the bolt
         via a curried send-budget function)

(ns dk.storm.throttle.bolt.scorekeeper
  (:use [backtype.storm clojure config])  
  (:require [clojure.string :as string])
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)  
  (:use dk.storm.throttle.score)
  (:use dk.storm.throttle.budget)
  (:require tron)
  (:use clojure.contrib.logging)  
  (:import [dk.storm.throttle.api Budget-Item Usage-Summary]))

(def cfg  (-> (config) :scorekeeper))

(def settings {
               :db-host (-> cfg :throttle-database :host)
               :db-port (-> cfg :throttle-database :port)
               :db-user (-> cfg :throttle-database :db-user)
               :db-password (-> cfg :throttle-database :db-password)
               :db-name (-> cfg :throttle-database :db-name)
               :cache-host (-> cfg :cache-host)
               :cache-port (-> cfg :cache-port)
               :cache-db-index (-> cfg :cache-db-index)
               :clock-period (-> cfg :sample-period)
               :delay(-> cfg :initial-delay)
              })

(def throttle-db {:classname "com.mysql.jdbc.Driver"
                   :subprotocol "mysql"
                   :subname (str "//" (:db-host settings) ":" (:db-port settings) "/" (:db-name settings))
                   :user (:db-user settings)
                :password (:db-password settings)})

(defmacro dbg[x] `(let [x# ~x] (println "dbg:" '~x "=" x#) x#))

(defn send-budget
  "sends budgets to delivery nodes"
  [collector scorekeeper]
  (warn "IN THE SEND-BUDGET  CALLBACK")
  (let [budget (new-budget throttle-db)
        nodes (active-nodes scorekeeper)
        sample-period (-> (config) :accumulate :sample-period)
        budget-items (get-budget-items scorekeeper)]
    (dbg budget)
    (doseq [item (calculate-budget budget budget-items nodes)]
      (emit-bolt! collector item))
    (tron/once #(send-budget collector scorekeeper) sample-period)))

;;                  (map->Budget-Item {:feed-id item :level-type item :level-id item :allotment item})))))


  
(defbolt score  ["summary"] {:prepare true}
  [conf context collector]
  (let [args (assoc settings :timer-cb (partial send-budget collector))
        sk (new-scorekeeper args)]
    (bolt
     (prepare [conf context collector]
              (warn "STARTING SCOREKEEPER USAGE")
              (start-clock sk))
     (execute [tuple]
              (warn "PROCESSING SUMMARY USAGE")
              (let [val (.getValue tuple 0)
                    fake-summary (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 105 :node 21})
                    usage-summary (map->Usage-Summary val)]
                (warn (dbg val))
                (process-usage-summary sk fake-summary))
;;                (process-usage-summary sk usage-summary))
              (ack! collector tuple)))))
