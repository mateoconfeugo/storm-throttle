(defproject dk-storm-throttle-topology "0.1.0-SNAPSHOT"
  :description "storm topology that reifies the real time throttling of it query requests to feed providers"
  :source-path "src"
  :main dk.storm.throttle.topology
  :javac-options {:debug "true" :fork "true"}
  :aot :all
  :jvm-opts ["-Djava.library.path=/usr/local/lib:/opt/local/lib:/usr/lib:/usr/lib/jvm/java-6-sun/lib"]
  :repositories {"twitter4j" "http://twitter4j.org/maven2"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [dk-storm-throttle-shared "0.1.4-SNAPSHOT"]
                 [org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.twitter4j/twitter4j-core "2.2.6-SNAPSHOT"]
                 [org.twitter4j/twitter4j-stream "2.2.6-SNAPSHOT"]
                 [kestrel-client "0.1.0"]
                 [overtone/at-at "1.0.0"]
                 [storm/storm-kestrel "0.7.2-snap2"]]
  :dev-dependencies [[storm "0.7.3"]
                     [cdt "1.2.6.2"]
                     [storm-test "0.2.0"]])
                 
