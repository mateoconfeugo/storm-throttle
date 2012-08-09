(ns dk.storm.throttle.parse
  (:use dk.storm.throttle.api)
  (:import [dk.storm.throttle.api Log-Record]))

(defprotocol Parse
  "Transforming log data into domain data"
  (extract-query [this log-record]
    "get the query from request record")
  (parse-log [this line]
    "Transforms the line into a map")
  (parse-query [this log-record]
    "extract throttle data from uri munged string"))

 ;;(def access-log-re-expn #"^([\d.]+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] \"(.+?)\ (\d{3}) (\d+|(.+?)) \"([^\"]+|(.+?))\" \"([^\"]+|(.+?))\"")


(def access-log-re-expn #"^([\d.]+) \[([\w:/]+\s[+\-]\d{4})\] \"([\w.]+) (.*)\,")
;; (\d{3}) (\d+|(.+?)) \"([^\"]+|(.+?))\" \"([^\"]+|(.+?))\"")

(def query-re-expn #"")

(defn parse-log-line
  "creates a log record by parsing a web server log line with a regex"
  [line]
  (let [tokens (re-seq access-log-re-expn line)
        record {:client-ip (get (first tokens) 1)
                :date (get (first tokens) 2)
                :method (get (first tokens) 3)
                :uri (get (first tokens) 4)
                :http (get (first tokens) 5)
                :code (get (first tokens) 6)                
                :number-bytes (get (first tokens) 7)
                :referer (get (first tokens) 8)
                :agent (get (first tokens) 9)}]
    (map->Log-Record record)))

(defn extract-usage-detail-record [log-record]
  (println "extract the data"))

                                        ; PARSE PROTOCOL REIFICATION 
(defn new-parser
  "object constructor"
  [config]
     (reify Parse
       (parse-log [this line]
         (parse-log-line line))

       (extract-query [this log-record]
         (extract-usage-detail-record log-record)

       (parse-query [this log-record]
                    (println "parseing query")))))



(comment
  (defn request-to-map [request]
    (keywordize-keys
     (apply hash-map
            (split request #"(&|=)"))))
  )