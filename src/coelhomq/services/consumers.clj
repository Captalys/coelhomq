(ns coelhomq.services.consumers
  (:require [langohr.core :as rmq]
            [langohr.consumers :as lc]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.basic :as lb]
            [coelhomq.services.config :refer [config]]
            [cheshire.core :refer [generate-string parse-string]]))

(defn- ack-or-nack
  [next-action chan delivery-tag]
  (case next-action
    :nack-requeue (lb/nack chan delivery-tag false true)
    :nack (lb/nack chan delivery-tag false false)
    :ack (lb/ack chan delivery-tag)
    (lb/nack chan delivery-tag false false)))

(defn never-decrease
  [ch {:keys [delivery-tag] :as meta} ^bytes payload]
  (let [body (String. payload)]
    (ack-or-nack :nack-requeue ch delivery-tag)))

(defn auto-ack-with-delay
  [ch {:keys [delivery-tag] :as meta} ^bytes payload]
  (let [{:keys [name age address]} (parse-string (String. payload) keyword)]
    (println (format "This guy named %s with age %s wants to live in %s" name age address))
    (Thread/sleep 1000)))

(defn manual-ack-with-delay
  [ch {:keys [delivery-tag] :as meta} ^bytes payload]
  (let [{:keys [name age address]} (parse-string (String. payload) keyword)]
    (println (format "This guy named %s with age %s wants to live in %s" name age address))
    (Thread/sleep 500)
    (ack-or-nack :ack ch delivery-tag)))

(defn never-ask-to-reprocess
  [ch {:keys [delivery-tag] :as meta} ^bytes payload]
  (let [{:strs [name age address]} (String. payload)]
    (ack-or-nack :nack ch delivery-tag)))

(def assignments
  [{:queue-name "manual-->never-decrease"
    :routing-key "never-decrease"
    :auto-ack false
    :fun never-decrease
    :consumer-tag "nd"}

   {:queue-name "manual-->never-ask-to-reprocess"
    :routing-key "never-ask-to-reprocess"
    :auto-ack false
    :fun never-ask-to-reprocess
    :consumer-tag "natr"}

   {:queue-name "manual-->ack-with-delay"
    :routing-key "manual-ack-with-delay"
    :auto-ack false
    :fun manual-ack-with-delay
    :consumer-tag "manual-ack"}
   
   {:queue-name "auto-->ack-with-delay"
    :routing-key "auto-ack-with-delay"
    :auto-ack true
    :fun auto-ack-with-delay
    :consumer-tag "auto-ack"}])

(def outputs (atom (list)))

(defn spin-consumer
  [{:keys [queue-name routing-key fun consumer-tag auto-ack]}]
  (let [conn (rmq/connect (:rabbitmq config))
        chan (lch/open conn)]
    (swap! outputs conj {:channel chan :consumer-tag consumer-tag})
    (lq/declare chan queue-name {:exclusive false
                                 :auto-delete false
                                 :durable true})
    (when-not auto-ack
      ;; amount of messages to send before receive an explicit ACK from consumer.
      (lb/qos chan 1))
    (lq/bind chan queue-name "coelhomq" {:routing-key routing-key})
    (lc/subscribe chan queue-name fun {:auto-ack auto-ack
                                       :consumer-tag consumer-tag})))

(defn start []
  (letfn [(consumer-tag [n] (str n "_" (str (java.util.UUID/randomUUID))))]
   (doseq [it assignments]
     (spin-consumer (update it :consumer-tag consumer-tag)))))

(defn stop []
  (doseq [{:keys [channel consumer-tag]} @outputs]
    (lb/cancel channel consumer-tag))
  (reset! outputs (list)))

(defn publish-msgs [routing-key data]
  (let [conn (rmq/connect (:rabbitmq config))
        chan (lch/open conn)]
    (lb/publish chan "coelhomq" routing-key data {:content-type "text/plain"})))

(def data {:name "Wand" :age "42" :address "Bolder"})

(def messages [{:routing-key "auto-ack-with-delay"
                :data data
                :amount 1000}
               {:routing-key "manual-ack-with-delay"
                :data data
                :amount 1000}
               {:routing-key "never-ask-to-reprocess"
                :data data
                :amount 1000}
               {:routing-key "never-decrease"
                :data data
                :amount 1000}])

(defn pub-now []
  (doseq [{:keys [routing-key data amount]} messages]
    (dotimes [_ amount]
      (publish-msgs routing-key (generate-string data)))))


;;; if you want to REPROCESS the messages you need to explicit tell rabbitmq to RESEND the messages again.
;;; for that matter is important to secure the channel used =) you will need it.

(defn send-again-noacked-msgs []
 (let [{:keys [channel]} (first (filter #(= "nd_" (subs (:consumer-tag %) 0 3)) @outputs))]
   (lb/recover channel true)))

(comment
  (start)
  (stop)
  (pub-now))
