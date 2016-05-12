(ns jungle.receiver.metric)

(defonce counter (atom 0))

;; ------------------------------------------------------------ update

(defn add-record
  [metrics metric time record]
  (assoc-in metrics [metric time] record))

(defn http-add-record
  [{{:keys [name timestamp value]} :params
    state :metrics-state}]
  (let [time (read-string timestamp)
        record (read-string value)]
    (swap! state add-record name time record)
    (swap! counter inc)
    "OK"))

;; ------------------------------------------------- aggregation query

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
  (let [ti (read-string from)
        tf (read-string to)]
    (aggregate @state name ti tf)))

;; ------------------------------------------------ metric names query

(defn http-names
  [{state :metrics-state}]
  (keys @state))

;; ----------------------------------------------- value at time query

(defn at
  [metrics metric time]
  (let [records (get metrics metric)
        descending (sort-by key > records)
        record (some 
                (fn [[t r]] (< t time))
                records)]
    (when record 
      (val record))))

(defn http-at
  [{{:keys [name time]} :params
    state :metrics-state}]
  (println time)
  (let [t (read-string time)]
    (at @state name t)))
