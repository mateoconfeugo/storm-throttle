(ns dk.storm.throttle.governor-proxy-client
  (:use [cheshire.core])
  (:use dk.storm.throttle.config)
  (:import (java.net Socket)
           (java.io PrintWriter InputStreamReader BufferedReader)))

(declare conn-handler)

(defn connect [server]
  (let [socket (Socket. (:name server) (:port server))
        in (BufferedReader. (InputStreamReader. (.getInputStream socket)))
        out (PrintWriter. (.getOutputStream socket))
        conn (ref {:in in :out out})]
    (doto (Thread. #(conn-handler conn)) (.start))
    conn))

(defn write [conn msg]
  (doto (:out @conn)
    (.println (str msg "\r"))
    (.flush)))

(defn conn-handler [conn]
  (while (nil? (:exit @conn))
    (dosync (alter conn merge {:exit true}))))

(defn throttled-nodes []
  "TODO: Fix this Retrieves a list of the nodes that should be throttled "
  (let [ nodes (-> (config) :governor :nodes)
        port (-> (config) :usage-detail :proxy)]
                                        ;TODO iterate through the nodes and construct a vector of maps
    ))

(def nodes (throttled-nodes))
(def user {:name "governor" :password "throttle_this"})
  
(defn publish-budget [budget-item]
  "Sends the updated budgets to the throttled delivery servers"
  (doseq [node nodes]
    (let [client (connect node)]
      (println budget-item)      
      (println (generate-string budget-item))      
      (write client (generate-string budget-item)))))
