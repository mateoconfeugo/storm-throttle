(ns dk.storm.throttle.test.budget
  (:use [dk.storm.throttle.score])
  (:use dk.storm.throttle.budget)
  (:use dk.storm.throttle.config)
  (:use dk.storm.throttle.api)
  (:import [dk.storm.throttle.api Budget-Item Usage-Summary Node-Usage-Summary Cluster-Usage-Summary])
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

(def summary-1 (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 105 :node 21}))
(def summary-2 (map->Usage-Summary {:feed-id 8087 :level-id 1 :level "source" :count 200 :node 21}))
(def summary-3 (map->Usage-Summary {:feed-id 8002 :level-id 1 :level "source" :count 300 :node 22}))
(def summary-4 (map->Usage-Summary {:feed-id 8003 :level-id 1 :level "source" :count 400 :node 23}))
(def summary-5 (map->Usage-Summary {:feed-id 8004 :level-id 1 :level "source" :count 500 :node 23}))
(def summary-6 (map->Usage-Summary {:feed-id 8004 :level-id 2 :level "source" :count 600 :node 24}))
(def test-summaries [summary-1 summary-3 summary-4 summary-5 summary-6])

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



(deftest calculate-budget-test
    (let [budget (new-budget throttle-db)
          test-result (calculate-budget budget test-budgets test-active-nodes)]
      (is (= (:count (first (test-result))) 600))))

(comment
(deftest distribute-test
  (let [budget (new-budget throttle-db)
        distributed-budget (distribute budget test-budgets test-active-nodes)]
    (dorun (distributed-budget)
           (is (= (:count (first (test-budgets))) 600)))))

(deftest get-budget-item-test
  (let [budget (new-budget throttle-db)
        feed-id 8002
        level-type "source"
        level-id   2
        node       "mp21"
        item (get-budget-item budget feed-id level-type level-id)]
    (is (= item budget-item-1))))

(deftest budget-items-test
  (let [budget (new-budget throttle-db)
        test-result (budget-items budget)]
    (is (= (first test-result) budget-item-1))))

(deftest apportionment-test
  (let [budget (new-budget throttle-db)
        test-result (apportionment budget test-count test-node-portion)]
  (is (= (first test-result) test-updated-budget-item))))
)
;(run-tests 'dk.storm.throttle.test.score)