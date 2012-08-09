(ns dk.storm.throttle.bolt.governor
  (:use [backtype.storm clojure config])
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.govern))

(defbolt update  ["id" "budget"] [tuple collector]
  (let [cfg (-> (config) :governor)
        cache (:budget-cache-path cfg)
        governor (new-governor cache)]
    (bolt
     (execute [tuple]
              (let [budget-item (.getString tuple 0)]
                ;; this may need to be transformed into a Budget-Item record
                (send governor budget-item)
                (ack! collector tuple))))))
