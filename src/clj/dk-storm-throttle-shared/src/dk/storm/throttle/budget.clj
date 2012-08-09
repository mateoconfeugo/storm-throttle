(ns dk.storm.throttle.budget
  (:use dk.storm.throttle.api)
  (:use dk.storm.throttle.config)
  (:use [clojure.java.jdbc])
  (:refer-clojure :exclude (resultset-seq))
  (:require [clojure.java.jdbc :as sql]))

(defn max-queries-per-second-per-feed
  "Given a feed determin the max amount of queires this node is budgeted"
  [{:keys [feed cache]}]
  (let []))

(defn node-query-ratio
  "Give a feed determin the max amount of queires this node is budgeted"
  [active-nodes]
  (let []))

(defn node-max-portion
  "Calculate the max amount of queries this node can server"
  [ratio count]
  (* (node-query-ratio  ) (max-queries-per-second-per-feed)))

(defn retrieve-blackouts
  "Gets the blackout periods for a given feed level combination in the flow"
    [{:keys[db feed-id level-id level]}])

(defprotocol Budget
  "Object that is used to populate the  end point object cache the client uses
   these serialized objects to decide whether or not perform some action such
   as throttle and to possible toreport back"
  (calculate-budget [this usage-summary-data active-nodes]
    "Determine the allocated budget of queries for a particualr id at a certian level in flow")
  (blackouts [this feed-id level-type level-id]
      "For this path through flow i.e feed source combo what black out periods are scheduled"))

(defn new-budget
  [throttle-db]  
  (reify Budget
      (calculate-budget [this budget-items active-nodes]
        (lazy-seq
         (map #(map->Budget-Item (assoc % :current-count (/ (:current-count %) active-nodes))) budget-items)))

      (blackouts [this feed-id level-type level-id]
        (retrieve-blackouts [throttle-db feed-id level-type level-id]))))

