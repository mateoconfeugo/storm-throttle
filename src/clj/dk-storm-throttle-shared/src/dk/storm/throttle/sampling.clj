(ns dk.storm.throttle.sampling)
  (comment
 (:use clj-time.now)
 (:use clj-time.local)
 )

(comment

(defn seconds-elapsed-today
  "Calculate how many of a days seconds have ticked by"
  []
  (let [current-hour (hour (local-now))
        current-minute (minute (local-now))]
    (+ (* current-hour 3600) (* current-minute 60) (sec (local-now)))))

(defn seconds-remaining-today
  "Calculate how many of a days 86400 seconds are left"
  []
   (- 86400 (seconds-elapsed-today)))

(defn sample-periods-elapsed-today
  "Calculate the number of sample periods completed for the day"
  [sample-period]
  (/ (seconds-elapsed-today) sample-period))

(defn sample-periods-remaining-today
  "Calculate the number of sample periods remaining for the day"  
  [sample-period]
  (/ (seconds-remaining-today) sample-period))

(defn current-sample-period
  "gets the period of the day currently in"
  [])

(defn blackout?
  "Determine if in a blackperiod"
  [blackout-schedule]
  (let [current-period (current-sample-period)
        current-hour (hour (local-now))]
    (get blackout-schedule current-hour)))

(defn blackout-compensation
  "How many extra query requests can be made total when not blacked out so as to increase the click level"
  [blackout-schedule & total]
  (let [hour-flag (first blackout-schedule)
        qph (qps-to-qph)]
    (when (pos? hour-flag)
      (recur (rest blackout-schedule) (+ qps total)))))

(defn unused-queries-remaining
  "Assuming level loading of click budget throughout the day from the previous sample period how many requests attempts were not used"
  [alloted actual]
  (- alloted actual))

(defn raw-average
  "The total alloment for a feed level pairing divided by the number of active nodes"
  [alloment number-nodes]
  (/ alloment number-nodes))

(defn query-request-allotment
  "For a sample period calculate the weighted distributed query alottement"
  [last-alloment node-total]
  (/ (+ (raw-average) (blackout-compensation) (unused-queries-remaining last-alloment)) node-total))

(defn qps-to-update-sample-period
  "Translate the number of quries per second allowed into queries per update sample periods"
  [qps sample-period])

(defn qps-to-qph
  "Translate the number of quries per second allowed into queries per hour"
  [qps])

(defn max-queries-rps
  "What is the set max query requests sent to the feed"
  [feed-id level leve-id])

(defn query-leveled-rps
  "What is the calcuated max request that will ensure the feed is available throughout the day - this adjusts"
  [feed-id level level-id])

(defn total-queries-per-day
  "For the feed level combination what is the total alloment of feed requests allowed for a particular consumer of the feed"
  [feed-id level level-id])


)