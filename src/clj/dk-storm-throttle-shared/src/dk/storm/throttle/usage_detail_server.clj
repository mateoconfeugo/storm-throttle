(ns dk.storm.throttle.usage-detail-server
 (:import [java.io BufferedReader InputStreamReader OutputStreamWriter])
 (:use clojure.contrib.server-socket))


(defn usage-detail-server 
  "simple multiplexing server that takes a json string of a usage detail record"
  [callback-fn host port]
  (letfn [(process-usage [in out]
            (binding [*in* (BufferedReader. (InputStreamReader. in))
                      *out* (OutputStreamWriter. out)]
              (loop []
                (let [input (read-line)]
                  (callback-fn input)
                  (flush))
                (recur))))]
    (create-server port process-usage)))

