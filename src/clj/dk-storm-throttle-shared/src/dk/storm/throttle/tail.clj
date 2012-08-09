(ns dk.storm.throttle.tail
  (:import java.io.RandomAccessFile)
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api))

(defprotocol Tail
  "tail a file or other resource"
  (follow [this]
    "works like the unix tail command continually returning new lines"))

(defn raf-seq
  [#^RandomAccessFile raf]
  (if-let [line (.readLine raf)]
    (lazy-seq (cons line (raf-seq raf)))
    (do (Thread/sleep 1000)
        (recur raf))))

(defn tail-seq [input]
  "reads the last line of the file"
  (let [raf (RandomAccessFile. input "r")]
    (.seek raf (.length raf))
    (raf-seq raf)))

(defn new-tail
  ([config]
     (reify Tail
       (follow [this]
         (tail-seq (-> (config) :path))))))

