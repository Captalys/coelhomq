(ns user
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
            [coelhomq.services.web :as web]
            [coelhomq.services.config :as config]
            [coelhomq.services.database :as database]))

(defn start []
  (mount/start #'config/config
               #'database/datasource
               #'web/server))

(defn stop []
  (mount/stop))

(defn refresh []
  (stop)
  (tn/refresh))

(defn go []
  (start)
  :ready)

(defn reset []
  (stop)
  (tn/refresh :after 'user/go))
