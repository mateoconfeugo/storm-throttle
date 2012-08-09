(ns dk.storm.throttle.test.budget
  (:use [dk.storm.throttle.governor-proxy-client)
  (:use dk.storm.throttle.budget)
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)
  (:import [dk.storm.throttle.api Budget-Item])
  (:use [clojure.test]))

(def cfg  (-> (config) :scorekeeper))

(def db-cfgs {
              :host (-> cfg :throttle-database :host)
              :port (-> cfg :throttle-database :port)
              :db-user (-> cfg :throttle-database :db-user)
              :db-password (-> cfg :throttle-database :db-password)
              :db-name (-> cfg :throttle-database :db-name)})

(def throttle-db {:classname "com.mysql.jdbc.Driver"
                   :subprotocol "mysql"
                   :subname (str "//" (:db-host db-cfgs) ":" (:db-port db-cfgs) "/" (:db-name db-cfgs))
                   :user (:db-user db-cfgs)
                   :password (:db-password db-cfgs)})

(def budget-item-1 (map->Budget-Item {:feed-id 8087 :level-id 1 :level "source" :current-count 100 :allotment 10000}))
(def budget-item-2 (map->Budget-Item {:feed-id 8087 :level-id 2 :level "source" :current-count 200 :allotment 10000}))
(def budget-item-3 (map->Budget-Item {:feed-id 8002 :level-id 1 :level "source" :current-count 300 :allotment 10000}))
(def budget-item-4 (map->Budget-Item {:feed-id 8003 :level-id 1 :level "source" :current-count 400 :allotment 10000}))
(def test-budgets [budget-item-1 budget-item-2 budget-item-3 budget-item-4])

(def test-active-nodes ["mp21" "mp22" "mp23" "mp24"])
(def test-budget (new-budget throttle-db))
(defn callback-test [] ())
(def settings (assoc db-cfgs :timer-cb callback-test :clock-period 100 :delay 1000 :cache-host "10.0.147.102" :cache-db-index (:cache-db-index cfg) :cache-port 6379))
(def test-sk (new-scorekeeper settings))
(def test-count {})
(def test-node-portion {:mp21 50 :mp22 50 :mp23 40 :mp24 50})
(def test-updated-budget-item (map->Budget-Item {:feed-id 8004 :level-id 2 :level "source" :count 1600}))

(deftest new-budget-test
  (let [budget (new-budget throttle-db)]))

