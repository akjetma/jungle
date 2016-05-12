(ns jungle.station
  "Simulates a process sending messages to a jungle receiver server."
  (:require [clojure.core.async :as a]
            [jungle.config :as config]
            [org.httpkit.client :as http]))

(defonce counter (atom 0))


(defn generator
  "returned function generates random numbers evenly distributed about 
  avg-val, between 2*avg-val and 0."
  [avg-val]
  (let [max-val (* 2 avg-val)]
    (fn gen
      []
      (rand-int max-val))))

(defn rate->timeout
  [avg-rate]
  (->> avg-rate   ;; req/s
       (/ 1)      ;; s/req
       (* 1000))) ;; ms/req

(defn send-record
  [metric time record]
  (swap! counter inc)
  (http/request
   {:method :post
    :url config/address
    :query-params {:name metric
                   :timestamp time
                   :value record}}))

(defn start-simulator
  [metric avg-val avg-rate]
  (let [stop-ch (a/chan)
        stop-fn #(a/put! stop-ch :stop)
        gen-timeout (generator (rate->timeout avg-rate))
        gen-value (generator avg-val)]
    (a/go-loop [timeout 0
                value 0]
      (let [[_ chan] (a/alts! [stop-ch (a/timeout timeout)])]
        (when-not (= chan stop-ch)
          (send-record metric
                       (System/currentTimeMillis)
                       value)          
          (recur 
           (gen-timeout)
           (gen-value)))))
    (println metric "simulator started")
    (fn stop-simulator
      []
      (a/put! stop-ch :stop)
      (println metric "simulator stopped"))))
