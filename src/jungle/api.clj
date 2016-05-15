(ns jungle.api
  (:require [clojure.data.json :as json]
            [clojure.stacktrace :as st]
            [clojure.set :as set]))

;; --------------------------------------------------------- responses

(defn success
  [value]
  {:status 200
   :body {:value value}})

(defn error
  [status error & [message]]
  {:status status
   :body (merge 
          {:error error}
          (when message 
            {:message message}))})

;; ---------------------------------------------------- misc. handlers

(defn handle-missing
  [{:keys [uri]}]
  (error 404 
         "Route Not Found" 
         (str "Requested: " uri)))

;; ----------------------------------------------------- test handlers

(defn success-test
  "Check format of good response"
  [_] 
  (success "Cool"))

(defn user-error-test
  "Check format of explicit error response"
  [_]
  (error 400 
         "Nope" 
         "No thanks"))

(defn server-error-test
  "Check that last-ditch exception handling middleware works for
  uncaught exceptions."
  [_]
  (/ 1 0))

(defn wrapper-test
  "Check endpoint-wrapper"
  [{{:keys [a b c]} :params}]
  (success (str a " " b " " c)))

;; ---------------------------------------------- endpoint middlewares

(defn wrap-type-check
  "Specify types for query params
  -------------------------------
  param->type ~= {<param name> <expected type>}
  -------------------------------------------------------"
  [app param->type]
  (fn [{:keys [params] :as request}]
    (if-let [[param value expected received] 
             (some
              (fn [[param expected]]
                (let [value (get params param)]
                  (when-not (instance? expected value)
                    [param value expected (type value)])))
              param->type)]
      (error 400
             "Invalid Type"
             (str "Parameter: " param
                  "\nValue: " value
                  "\nType: " received
                  "\nExpected: " expected))
      (app request))))

(defn wrap-parse-params
  "Specify parsing functions for query params
  -------------------------------------------
  param->parse ~= {<param name> <parsing function>}
  -------------------------------------------------"
  [app param->parse]
  (fn [request]
    (let [parsed
          (try
            (reduce
             (fn [r [param parse]]
               (update-in r [:params param] parse))
             request
             param->parse)
            (catch java.lang.NumberFormatException e
              nil))]
      (if parsed
        (app parsed)
        (error 400
               "Parsing Error"
               (str "Could not parse parameters: " (:params request)))))))

(defn wrap-required-params
  "Specify required params
  ------------------------
  required ~= #{<param name>}
  ---------------------------"
  [app required]
  (fn [{:keys [params] :as request}]
    (let [provided (keys params)
          missing (set/difference (set required) (set provided))]
      (if (empty? missing)
        (app request)
        (error 400
               "Missing Required Parameters"
               (str "Required: " required
                    ", Provided: " provided
                    ", Missing: " missing))))))

(defn wrap-endpoint
  "Combination of middlewares for api query endpoints
  ---------------------------------------------------
  {:required #{:param1 :param2}
   :parse {:param1 read-string
           :param3 string/lower-case}
   :types {:param1 java.lang.Long}
  ------------------------------"
  [app {:keys [required parse types]}]
  (cond-> app
    types (wrap-type-check types)
    parse (wrap-parse-params parse)
    required (wrap-required-params required)))

;; --------------------------------------------- top-level middlewares

(defn wrap-missing
  "My routing library doesn't do wildcard route matching, so this needs
  to be placed in the top-level middleware stack to control the response
  for missing routes."
  [app]
  (fn [request]
    (let [response (app request)]
      (if (= 404 (:status response))
        (handle-missing request)
        response))))

(defn wrap-stacktrace
  "Since this service would be used internally, I thought it would be
  useful to send exception stacktraces in the response body for unhandled
  exceptions."
  [app]
  (fn [request]
    (try
      (app request)
      (catch Exception e
        (let [trace (with-out-str (st/print-stack-trace e))]
          (println trace)
          (error 500 
                 "Uncaught exception" 
                 trace))))))

(defn wrap-json
  [app]
  (fn [request]
    (-> (app request)
        (update :body json/write-str)
        (assoc-in [:headers "Content-Type"] "application/json"))))

(defn wrap-assoc-request
  [app k v]
  (fn [request]
    (let [updated (assoc request k v)]
      (app updated))))
