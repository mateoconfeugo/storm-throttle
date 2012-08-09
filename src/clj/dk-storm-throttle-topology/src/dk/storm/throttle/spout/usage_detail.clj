(ns dk.storm.throttle.spout.usage-detail
  (:import [backtype.storm StormSubmitter LocalCluster])
  (:use [backtype.storm clojure config])
  (:use dk.storm.throttle.config)
;;  (:use kestrel.client)
  (:use dk.storm.throttle.usage-detail-server))

(defn inject-into-system  
  "callback function the server uses to inject the tuples into the system"
  [collector usage-detail-record]
  (emit-spout! collector [usage-detail-record]))

(defspout usage-detail-flow  ["usage-detail"]
  [conf context collector]
  (let [callback-fn (partial inject-into-system collector)
        host (-> (config) :usage-detail :host)
        port (-> (config) :usage-detail :port)]
    (spout
     (nextTuple []
                (usage-detail-server callback-fn host port))
     (ack [id]))))

(comment
(defspout usage-detail-queue ["usage-detail"]
  [conf context collector]
  (let [host (-> (config) :usage-detail :host)
        port (-> (config) :usage-detail :port)
        q-name (-> (config) :usage-detail :qname)
        queue (default-client  :host :port)]
    (spout
     (nextTuple []
                (emit-spout! collector (get-item q-name))))))
)
     
