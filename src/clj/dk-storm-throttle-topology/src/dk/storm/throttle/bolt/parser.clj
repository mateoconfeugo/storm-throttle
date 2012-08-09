(ns dk.storm.throttle.bolt.parser
  (:use [backtype.storm clojure config])
  (:use dk.storm.throttle.config)  
  (:use dk.storm.throttle.parse))

                        
(defbolt generate-usage-detail  ["id" "budget"] [tuple collector]
  (let [parser (new-parser (-> (config) :parser))
        line ((.getString tuple 0))
        log-record (parse-log parser line)
        query (extract-query parser log-record)
        usage (parse-query parser query)]
    (emit-bolt! collector usage)
    (ack! collector tuple)))
