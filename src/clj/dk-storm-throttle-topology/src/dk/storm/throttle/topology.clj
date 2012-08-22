(ns dk.storm.throttle.topology
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:use [backtype.storm clojure config])
  (:import [backtype.storm.spout KestrelThriftClient KestrelThriftSpout])
  (:import [backtype.storm.scheme StringScheme])
  (:use dk.storm.throttle.config)
  (:use kestrel.client)
  (:use dk.storm.throttle.spout.usage-detail)
  (:use dk.storm.throttle.bolt.parser)
  (:use dk.storm.throttle.bolt.accumulator)
  (:use dk.storm.throttle.bolt.scorekeeper)
  (:use dk.storm.throttle.bolt.governor)
  (:use clojure.contrib.logging)
  (:use dk.storm.throttle.api)      
  (:use dk.storm.throttle.accumulate)
  (:import [dk.storm.throttle.api Usage-Detail Usage-Summary])
  
  (:gen-class))
                                        ; TOPOLOGY

(defn mk-topology []
  "The usage spout streams usage-detail records recieved from feed server to the
   accumlator bolt which then totals up all the usage across the nodes.
   This summary is  emitted to the scorekeeprer bolt. The scorekeeper
   assembles a budget of allocated query request and current count for the feed source tuples.
   These budget item tuples are emitted to the govenor bolt that updates the budget cache
   that the feed cgi via a tcp client that sends the jsonified tuples to the governor proxy 
   running on the throttle query server"
  (topology
   {"usage" (spout-spec usage-spout :p 1)}
   {"count" (bolt-spec {"usage" :shuffle}
                   count-usage
                   :p (-> (config) :accumulator :p))
    "allocate" (bolt-spec {"count" :shuffle}
                   score
                   :p (-> (config) :scorekeeper :p))
    "govern" (bolt-spec {"allocate" :shuffle}
                   update
                   :p (-> (config) :governor :p ))}))

(defn run-local! []
  (let [cluster (LocalCluster.)]
    (.submitTopology cluster "query-count" {TOPOLOGY-DEBUG true} (mk-topology))
    (Thread/sleep 10000)
    (.shutdown cluster)))

(defn -main
  "Send the topology that reifies the throttling system to the storm cluster"
  [topology-name]
  (warn "STARTING THROTTLING TOPOLOGY")
  (StormSubmitter/submitTopology
   topology-name
   {TOPOLOGY-DEBUG true
    TOPOLOGY-WORKERS 3}
   (mk-topology)))
