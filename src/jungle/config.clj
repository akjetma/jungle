(ns jungle.config
  "Shared information between the simulator and server. 
  (wouldn't hardcode this IRL)")

(def path "record/metric")
(def port 5000)
(def address (str "http://localhost:" port "/" path))

