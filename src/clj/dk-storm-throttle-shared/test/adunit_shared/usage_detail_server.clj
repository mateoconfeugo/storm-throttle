(ns dk.storm.throttle.test.usage-detail-server
  (:use [dk.storm.throttle.config])
  (:use [dk.storm.throttle.usage-detail-server])
  (:use [clojure.test]))

(defn hello
  ""
  [name]
  (println (str "hello:" name "would like some ham")))

(usage-detail-server hello "localhost" 21012)