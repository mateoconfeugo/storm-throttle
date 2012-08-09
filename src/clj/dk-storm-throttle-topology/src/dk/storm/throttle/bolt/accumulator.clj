(ns dk.storm.throttle.bolt.accumulator
  (:use [backtype.storm clojure config])
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)      
  (:use dk.storm.throttle.accumulate)
  (:use clojure.contrib.logging)
  (:import [dk.storm.throttle.api Usage-Detail Usage-Summary]))

;; IDIOMS:  Notice how the curried summary-cb function connects the accumulator and callback function
;; thus keeping the accumulator from depending on storm but allowing the accumulator to
;; emit tuples into storm at period intervals

(defn send-summary
  "stream the counts map back to connected bolt"
  [collector accumulator]
  (let [summaries (summarize accumulator)
        sample-period (-> (config) :accumulate :sample-period)]
    (doseq [summary summaries]
      (emit-bolt! collector [{:feed-id (:feed-id summary)
                              :level-id (:level-id summary)
                              :level (:level summary)
                              :count (:count summary)
                              :node (:node summary)}]))
      ;; (let [summary (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 105 :node 21})]
    (tron/once #(send-summary collector accumulator) sample-period)))

(defbolt count-usage  ["udr"] {:prepare true}
  [conf context collector]
  (let [cfg (-> (config) :accumulate)
        host (:count-cache-host cfg)
        port (:count-cache-port cfg)
        index (:count-cache-db-index cfg)
        clock-period (:sample-period cfg)
        delay (:initial-delay cfg)
        timer-cb (partial send-summary collector)
        accumulator (new-accumulator "10.0.147.102" 6379 6 5000 2000 timer-cb)]
    (bolt
     (prepare [conf context collector]
              (warn "IN THE PREPARE METHOD OF THE COUNT USAGE BOLT DARN IT")
             (start-clock accumulator))         
     (execute [tuple]
              (update-count accumulator (.getString tuple 0))
              (ack! collector tuple)))))
  
