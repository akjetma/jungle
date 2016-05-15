(ns jungle.metric
  (:require [jungle.api :as api]))

;; ------------------------------------------------------------ update

(defn record-metric
  [metrics name timestamp value]
  (assoc-in metrics [name timestamp] value))

(defn http-record-metric
  [{{:keys [name timestamp value]} :params
    state :metrics-state}]
  (swap! state record-metric name timestamp value)
  (api/success "OK"))

;; ------------------------------------------------------- aggregation

(defn filter-keys
  "like select-keys but with a predicate rather than a keyseq"
  [keypred m]
  (into
   {}
   (filter
    (comp keypred key)
    m)))

(defn between?
  [begin end v]
  (<= begin v end))

(defn aggregate
  "sum the values of a metric within a timerange (inclusive)"
  [metrics name from to]
  (->> (get metrics name)
       (filter-keys (partial between? from to))
       vals
       (reduce +)))

(defn http-aggregate
  [{{:keys [name from to]} :params
    state :metrics-state}]
  (api/success
   (aggregate @state name from to)))

;; ------------------------------------------------------ metric names

(defn http-names
  [{state :metrics-state}]
  (api/success
   (keys @state)))

;; ----------------------------------------------------- value at time

(defn at
  "gets the closest known value for metric before or at time"
  [metrics name timestamp]
  (let [records (get metrics name)
        descending (sort-by key > records)]
    (some
     (fn [[t value]] 
       (when (<= t timestamp) 
         value))
     descending)))

(defn http-at
  [{{:keys [name timestamp]} :params
    state :metrics-state}]
  (api/success 
   (at @state name timestamp)))
