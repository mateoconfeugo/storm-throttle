(defproject dk-storm-throttle-shared "0.1.4-SNAPSHOT"
  :description "storm-throttle functionality library"
  :aot :all
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/java.jdbc "0.1.1"]
                 [com.sleepycat/je "4.0.92"]
                 [cascalog/carbonite "1.3.0"]
                 [cupboard "1.0.0-SNAPSHOT"]
                 [joda-time "1.6.2"]
                 [dougselph/clj-time "0.4.3-SNAPSHOT-MOD"]
                 [com.esotericsoftware.kryo/kryo "2.16"]
                 [mysql/mysql-connector-java "5.1.6"]
                 [clj-record "1.1.1"]
                 [cheshire "4.0.0"]                 
                 [overtone/at-at "1.0.0"]
                 [lamina "0.4.1"]
                 [aleph "0.2.1-rc4"]
                 [gloss "0.2.1"]
                 [tron "0.5.3"]
                 [org.clojars.tavisrudd/redis-clojure "1.3.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [
                     [cdt "1.2.6.2"]
                     [clj-stacktrace-alex "0.2.5"]
                     ])

