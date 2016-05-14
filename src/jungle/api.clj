(ns jungle.api
  (:require [clojure.data.json :as json]
            [clojure.stacktrace :as st]
            [clojure.set :as set]))

;; ----------------------------------------- high level response types

(defn success
  [value]
  {:status 200
   :body {:value value}})

(defn error
  [status error & [message]]
  (let [body (when message {:message message})
        body (assoc body :error error)]
    {:status status
     :body body}))

;; ------------------------------------------------ misc/test handlers

(defn success-test
  [_] 
  (success "Cool"))

(defn user-error-test
  [_]
  (error 400
         (str "What did you do")
         "Explicit error response sent from the code"))

(defn server-error-test
  [_]
  (/ 1 0))

(defn parse-params-test
  [{{:keys [test-a]} :params}]
  (if (number? test-a)
    (success "Passed")
    (error 500
           "Parsing error"
           (str "test-a is a " (type test-a)))))

(defn query-endpoint-test
  [{{:keys [a b c]} :params}]
  (success (str a " " b " " c)))

(defn handle-missing
  [{:keys [uri]}]
  (error 404
         (str "Route not found: " uri)))

;; ------------------------------------------------------- middlewares

(defn wrap-validate-params
  "Specify validations for query params
  -------------------------------------
  param->validate ~= {<param name> <validation function>}
  -------------------------------------------------------"
  [app param->validate]
  (fn [{:keys [params] :as request}]
    (if-let [invalid (some
                      (fn [[k validate]]
                        (let [v (get params k)]
                          (when-not (validate v)
                            [k v validate])))
                      param->validate)]
      (error 400
             (str "Invalid value for parameter")
             (str "parameter: " (first invalid)
                  " value: " (second invalid)
                  " test: " (last invalid)))
      (app request))))

(defn wrap-parse-params
  "Specify parsing functions for query params
  -------------------------------------------
  param->parse ~= {<param name> <parsing function>}
  -------------------------------------------------"
  [app param->parse]
  (fn [request]
    (let [parsed
          (reduce
           (fn [r [param parse]]
             (update-in r [:params param] parse))
           request
           param->parse)]
      (app parsed))))

(defn wrap-missing-params
  "Specify required params. (note: required is a set)
  ---------------------------------------------------
  required ~= #{<param name>}
  ---------------------------"
  [app required]
  (fn [{:keys [params] :as request}]
    (let [provided (set (keys params))]
      (if (set/subset? required provided)
        (app request)
        (let [missing (set/difference required provided)]
          (error 400
                 "Missing Required Parameters"
                 (str "Required: " required ", Missing: " missing)))))))

(defn wrap-endpoint
  "Combination of middlewares for api query endpoints
  ---------------------------------------------------
  {:required #{:param1 :param2}
   :parsers {:param1 read-string
             :param3 string/lower-case}
   :validations {:param1 numeric?}}
  ---------------------------------"
  [app {:keys [required parse validate]}]
  (cond-> app
    validate (wrap-validate-params validate)
    parse (wrap-parse-params parse)
    required (wrap-missing-params required)))

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
          (error 500 "Uncaught exception" trace))))))

(defn wrap-json
  [app]
  (fn [request]
    (-> (app request)
        (update :body json/write-str)
        (assoc-in [:headers "Content-Type"] "application/json"))))

(defn assoc-request
  [app k v]
  (fn [request]
    (let [updated (assoc request k v)]
      (app updated))))
