(ns dk.storm.throttle.test.govern
  (:use [dk.storm.throttle.api])
  (:use [dk.storm.throttle.config])
  (:import [dk.storm.throttle.api Budget-Item Usage-Detail])  
  (:use [dk.storm.throttle.govern])
  (:require [cupboard.core :as cb])
  (:use cupboard.bdb.je)
  (:use [cupboard.utils])  
  (:use [clojure.test])
  (:use lamina.core)
  (:use aleph.formats)
  (:use aleph.tcp)
  (:use gloss.core)  
  (:use [cheshire.core]))

;; probably move to the api
(def cache-path (-> (config) :governor  :budget-cache-path))
(def query-nodes (-> (config) :governor  :query-nodes))
(def proxy-port (-> (config) :governor  :proxy-port))
(def dbindex (-> (config) :governor  :dbindex))

(def budget-item-1 (map->Budget-Item {:feed-id 8087 :level-id 1 :level "source" :current-count 100 :allotment 10000}))
(def budget-item-2 (map->Budget-Item {:feed-id 8087 :level-id 2 :level "source" :current-count 200 :allotment 10000}))
(def budget-item-3 (map->Budget-Item {:feed-id 8002 :level-id 1 :level "source" :current-count 300 :allotment 10000}))
(def budget-item-4 (map->Budget-Item {:feed-id 8003 :level-id 1 :level "source" :current-count 400 :allotment 10000}))
(def test-budgets [budget-item-1 budget-item-2 budget-item-3 budget-item-4])
(def governor (new-governor cache-path proxy-port query-nodes dbindex))

(comment

(def test-cache (cb/open-cupboard "/home/matt/data/bdb/budget/items"))

(def restored-item (get-budget-item budget))

(deftest new-governor-test
  (let [test-this (new-governor  cache-path)
        test-result (update-budget-item  test-this budget-item-1)
        stored-in-cache (parse-string (first (cb/query (= :feed-id 8001) (= :level "source") (:level-id 2) :struct budget :cupboard test-cache)))]
    (is (= test-result budget-item-1))))
)

;(run-tests 'dk.storm.throttle.test.govern)