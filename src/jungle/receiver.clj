(ns jungle.receiver
  "Receive metrics from quartet servers and present them for inspection."
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.stacktrace :as st]
            [jungle.config :as config]
            [jungle.metric :as metric]
            [org.httpkit.server :as http]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [polaris.core :as polaris]))

(defonce metrics-state (atom nil))
(defonce server-state (atom nil))

(defn wrap-api-response
  [app]
  (fn [request]
    (try
      (let [result {:success true :value (app request)}]
        {:status 200
         :body (json/write-str result)})
      (catch Exception e
        (let [result {:success false :error (.getMessage e)}]
          (st/print-stack-trace e)
          {:status 500
           :body (json/write-str result)})))))

(defn assoc-request
  [app k v]
  (fn [request]
    (let [updated (assoc request k v)]
      (app updated))))

(defn assoc-response
  [app k v]
  (fn [request]
    (let [response (app request)]
      (assoc response k v))))

(defn api-test [request] "hey")

(def routes 
  [[config/path :add-record metric/http-add-record]
   ["aggregate" :aggregate metric/http-aggregate]
   ["names" :names metric/http-names]
   ["at" :value-at metric/http-at]
   ["test" :test api-test]])

(def router
  (-> routes
      polaris/build-routes
      polaris/router
      wrap-api-response
      (assoc-request :metrics-state metrics-state)
      (assoc-response :headers {"Content-Type" "application/json"})
      wrap-keyword-params
      wrap-params))

(defn stop-server
  []
  (when-let [stop-fn @server-state]
    (stop-fn :timeout 100)
    (println "server stopped")
    (reset! server-state nil)))

(defn start-server
  []
  (stop-server)
  (let [new-server (http/run-server #'router {:port config/port})]
    (println "server started on port" config/port)
    (reset! server-state new-server)))

(defn -main
  []
  (start-server))
