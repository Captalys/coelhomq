(ns user
  (:require [mount.core :as mount]
            [clojure.tools.namespace.repl :as tn]
            [coelhomq.services.config :as config]))

(defn start []
  (mount/start #'config/config))

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
