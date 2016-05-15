(ns jungle.receiver
  "Receive metrics from quartet servers and present them for inspection."
  (:gen-class)
  (:require [clojure.edn :as edn]
            [jungle.api :as api]
            [jungle.config :as config]
            [jungle.metric :as metric]
            [org.httpkit.server :as http]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [polaris.core :as polaris]))

(defonce ^:private metrics-state
  ;; ---------------------------
  ;; The metrics 'database'. Gets assoced into the ring request map as
  ;; :metrics-state via middleware in router definition below.
  ;; ---------------------------------------------------------
  ;; {<metric name> {<unix timestamp ms> <value at timestamp>}}
  ;; ----------------------------------------------------------
  ;; {"some-metric" {1463192884101 1
  ;;                 1463192887224 2}
  ;;  "another-metric" {1463192884342 90
  ;;                    1463193090029 60}}
  ;; -------------------------------------
  (atom {}))

(defn parse-long
  [s]
  (Long. s))

;; --------------------------------------------- api endpoint handlers

(def record-metric
  (api/wrap-endpoint 
   metric/http-record-metric
   {:parse {:timestamp parse-long
            :value parse-long}}))

(def aggregate
  (api/wrap-endpoint 
   metric/http-aggregate 
   {:required #{:from :to :name}
    :parse {:from parse-long
            :to parse-long}
    :types {:from Number
            :to Number
            :name String}}))

(def names
  (api/wrap-endpoint 
   metric/http-names 
   {}))

(def at
  (api/wrap-endpoint 
   metric/http-at 
   {:required #{:name :timestamp}
    :parse {:timestamp parse-long}
    :types {:timestamp Number
            :name String}}))

(def wrapper-test
  (api/wrap-endpoint 
   api/wrapper-test 
   {:required #{:a :b :c}
    :parse {:a parse-long
            :b parse-long}
    :types {:a Number
            :c String}}))

;; ------------------------------------------------------------ server

(def routes 
  [[config/path :record-metric record-metric]
   ["query" :query api/handle-missing
    [["aggregate" :aggregate aggregate]
     ["names" :names names]
     ["at" :at at]]]
   ["test" :test api/handle-missing
    [["wrapper" :wrapper wrapper-test]
     ["success" :success api/success-test]
     ["user-error" :user-error api/user-error-test]
     ["server-error" :server-error api/server-error-test]]]])

(def router
  (-> routes
      polaris/build-routes
      polaris/router
      api/wrap-missing
      api/wrap-stacktrace
      api/wrap-json
      (api/wrap-assoc-request :metrics-state metrics-state)
      wrap-keyword-params
      wrap-params))

(defonce server-stop-fn
  (atom (constantly nil)))

(defn stop-server
  []
  (@server-stop-fn :timeout 100)
  (println "server stopped")
  (reset! server-stop-fn (constantly nil)))

(defn start-server
  []
  (stop-server)
  (let [stop-fn (http/run-server #'router {:port config/port})]
    (println "server started on port" config/port)
    (reset! server-stop-fn stop-fn)))

(defn -main
  []
  (start-server))
