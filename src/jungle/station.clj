(ns jungle.station
  "Send metrics to the jungle receiver."
  (:require [clojure.core.async :as a :refer [go go-loop]]
            [environ.core :as environ]
            [org.httpkit.client :as http]))

(def receiver-path "/metric")

(defn current-timestamp
  "Using GMT epoch time for now. Wrapping in function in case I change later."
  []
  (System/currentTimeMillis))

(defn metric-msg
  "format/serialize metric data"
  [timestamp metric status]
  (prn-str
   {:timestamp timestamp
    :name metric
    :value status}))

(defn report
  "send metric to jungle receiver"
  [receiver metric status]
  (let [timestamp (current-timestamp)
        message (metric-msg time metric status)]
    (println "reporting" metric "at" timestamp ":" status "to" receiver)
    #_
    (http/request
     {:method :post
      :url receiver
      :body message})))

(defn start-reporter
  "Starts reporting service. Returns functions for stopping service and setting
  the status of a metric. Sends most recent metric status to jungle server at a
  maximum interval of [interval] ms. Effectively caps the maximum number of
  messages sent to jungle receiver and decouples setting the metric's status from 
  reporting it."
  [receiver metric interval]
  (let [stop-ch (a/chan)
        ;; since we're sending messages at regular intervals and only want the
        ;; latest information, we can drop stale statuses using a sliding buffer.
        status-ch (a/chan (a/sliding-buffer 1))]
    (go-loop []
      (let [[_ chan] (a/alts! [stop-ch (a/timeout interval)])]
        (if (= chan stop-ch)
          (println "stopping metric reporter")
          (let [status (a/poll! status-ch)]
            ;; if there's no message here, we have no information for this
            ;; interval.
            (when status 
              (report receiver metric status))
            (recur)))))
    {:stop-reporter (fn stop-reporter 
                      [] 
                      (a/put! stop-ch :stop))
     :set-status (fn set-status 
                   [status] 
                   (a/put! status-ch status))}))

(defn system-reporter
  "Convenience function for starting the primary metric for a service 
  (its hearbeat) from environment variables."
  []
  (let [{receiver-root :jungle-receiver 
         metric :jungle-metric 
         interval :jungle-interval} environ/env
        receiver (str receiver-root receiver-path)]
    (when (every? some? [receiver-root metric interval])
      (start-reporter receiver metric interval))))
