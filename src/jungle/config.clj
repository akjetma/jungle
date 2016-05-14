(ns jungle.config
  "Shared information between the simulator and server.
  
  In a large app or one with a system-level testing suite, these values would 
  be provided as parameters to functions, stuartsierra component-style.")

(def path "record-metric")
(def port 5000)
(def address (str "http://localhost:" port "/" path))

