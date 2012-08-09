(ns dk.storm.throttle.test.accumulate
  (:use [dk.storm.throttle.config])
  (:use dk.storm.throttle.api)
  (:use [overtone.at-at])
  (:use [dk.storm.throttle.accumulate])
  (:import [dk.storm.throttle.api Usage-Detail])
  (:use [clojure.test]))

(defn test-callback-function
  [accumulator]
  (println (summarize accumulator))
  (println "LOOK HERE"))

(def sample-query {})
(def sample-summary {})

(def sample-udr   (map->Usage-Detail {:feed-id 8087 :level "source" :level-id 1 :query sample-query}))
(def sample-udr-1 (map->Usage-Detail {:feed-id 8001 :level "source" :level-id 2 :query sample-query}))
(def sample-udr-2 (map->Usage-Detail {:feed-id 8002 :level "source" :level-id 3 :query sample-query}))
(def sample-udr-3 (map->Usage-Detail {:feed-id 8003 :level "source" :level-id 4 :query sample-query}))
(def sample-udr-4 (map->Usage-Detail {:feed-id 8003 :level "asp" :level-id 1 :query sample-query}))
(def sample-udr-5 (map->Usage-Detail {:feed-id 8003 :level "publisher" :level-id 2 :query sample-query}))

(def ham  (new-accumulator "10.0.147.102" 6379 6 5000 2000 test-callback-function))

(deftest summary-test
  (let [test-this (new-accumulator "10.0.147.102" 6379 6 5000 2000 test-callback-function)
        test-summary (summarize test-this)]
    (is (= test-summary sample-summary))))

(deftest test-update-usage-count
  (let [test-this (new-accumulator "10.0.147.102" 6379 6 5000 2000 test-callback-function)]
    (update-count test-this sample-udr)))
    
