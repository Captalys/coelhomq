(ns coelhomq.server
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [coelhomq.services.config :as config])
  (:gen-class))

(defn -main []
  "Entry point function to start the whole application."
  (mount/start #'config/config)
  (log/info ::api.flying))
