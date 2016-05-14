(ns jungle.metric
  (:require [jungle.api :as api]))

;; ------------------------------------------------------------ update

(defn add-record
  [metrics metric time record]
  (assoc-in metrics [metric time] record))

(defn http-add-record
  [{{:keys [name timestamp value]} :params
    state :metrics-state}]
  (swap! state add-record name timestamp value)
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

(defn aggregate
  "sum the values of a metric within a timerange (inclusive)"
  [metrics metric from to]
  (->> (get metrics metric)
       (filter-keys #(<= from % to))
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
  [metrics metric time]
  (let [records (get metrics metric)
        descending (sort-by key > records)
        record (some 
                (fn [[t r]] 
                  (and (< t time) r))
                descending)]
    record))

(defn http-at
  [{{:keys [name time]} :params
    state :metrics-state}]
  (api/success 
   (at @state name time)))
