(ns jungle.metric
  (:require [jungle.api :as api]))

;; ------------------------------------------------------------ update

(defn record-metric
  [metrics name timestamp value]
  (assoc-in metrics [name timestamp] value))

(defn record-metric-handler
  [{{:keys [name timestamp value]} :params
    state :metrics-state}]
  (swap! state record-metric name timestamp value)
  (api/success "OK"))

(def record-metric-endpoint
  (api/wrap-endpoint 
   record-metric-handler
   {:parse {:timestamp api/parse-long
            :value api/parse-long}}))

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

(defn aggregate-handler
  [{{:keys [name from to]} :params
    state :metrics-state}]
  (api/success
   (aggregate @state name from to)))

(def aggregate-endpoint
  (api/wrap-endpoint 
   aggregate-handler
   {:required #{:from :to :name}
    :parse {:from api/parse-long
            :to api/parse-long}
    :types {:from Number
            :to Number
            :name String}}))

;; ------------------------------------------------------ metric names

(defn names-handler
  [{state :metrics-state}]
  (api/success
   (keys @state)))

(def names-endpoint
  (api/wrap-endpoint 
   names-handler
   {}))

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

(defn at-handler
  [{{:keys [name timestamp]} :params
    state :metrics-state}]
  (api/success 
   (at @state name timestamp)))

(def at-endpoint
  (api/wrap-endpoint 
   at-handler
   {:required #{:name :timestamp}
    :parse {:timestamp api/parse-long}
    :types {:timestamp Number
            :name String}}))
