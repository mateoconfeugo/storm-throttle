(defproject dk-storm-throttle-governor-proxy "0.1.0"
  :description "server that takes budget items from the throttling storm
                topology and inserts them into the in-memory cache"
  :main dk.storm.throttle.governor-proxy
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [dk-storm-throttle-shared "0.1.0-SNAPSHOT"]])

