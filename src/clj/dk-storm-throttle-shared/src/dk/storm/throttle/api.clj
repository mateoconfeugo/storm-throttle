(ns dk.storm.throttle.api
  (:use [carbonite api buffer serializer])
  (:import [java.nio ByteBuffer]
           [java.net URI]
           [java.util Date UUID]
           [java.util.regex Pattern]
           [com.esotericsoftware.kryo Kryo Serializer]))


                                        ; DOMAIN ABSTRACT DATA TYPES
(defrecord Blackout-Schedule[schedule])
(defrecord Feed [id])
(defrecord Feed-Config [total budgeted blackout])
(defrecord Query-Delivery-Node [hostname ip status])
(defrecord Log-Entry [line])
(defrecord Log-Record [client-ip date method uri http-code number-bytes referer agent])
(defrecord Query [keyword level id] )
(defrecord Query-Summary [level id queries-received])
(defrecord Usage-Detail [feed-id level level-id query])
(defrecord Usage-Count [feed-id level level-id count])
(defrecord Usage-Summary [feed-id level level-id count node])
(defrecord Node-Usage-Summary [node-id level level-id queries-received])
(defrecord Cluster-Usage-Summary [nodes level level-id total-queries-received])
(defrecord Budget-Item [feed-id level-id level allotment current-count])

(def usage-summary-serializer
  (proxy [Serializer] []
    (writeObjectData [buffer summary]
      (clj-print buffer (:feed-id summary))
      (clj-print buffer (:level summary))
      (clj-print buffer (:level-id summary))
      (clj-print buffer (:count summary))
      (clj-print buffer (:node summary)))
    (readObjectData [buffer type]
      (Usage-Summary. (clj-read buffer)
                      (clj-read buffer)
                      (clj-read buffer)
                      (clj-read buffer)
                      (clj-read buffer)))))

(register-serializers (default-registry) {Usage-Summary usage-summary-serializer})
