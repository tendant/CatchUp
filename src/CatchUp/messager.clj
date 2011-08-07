(ns CatchUp.messager
  (:require [com.mefesto.wabbitmq :as mq]
            [CatchUp.gtalk :as gtalk]
            [CatchUp.couch :as couch]
            [cheshire.core :as json]
            ))


;;; RabbitMQ

(def mq-config {:host "localhost" :username "guest" :password "guest"})

(defn broker
  []
  (mq/with-broker mq-config 
    (mq/with-channel
      (mq/exchange-declare "catchup.exchange" "direct")
      (mq/queue-declare "notify.queue")
      (mq/queue-bind "notify.queue" "catchup.exchange" "notify"))))

(defn publisher
  [route message]
  (mq/with-broker mq-config
    (mq/with-channel
      (mq/with-exchange  "catchup.exchange"
        (println "publisher: " (.getBytes message))
        (mq/publish route (.getBytes message)))))
  )

(defn parse-notification
  [message]
  (let [{user_id "user_id", item_id "item_id"} (json/parse-string message)
        user (couch/get-item user_id)
        item (couch/get-item item_id)]
    {:user_id user_id
     :gtalk (get-in user [:user :gtalk])
     :description (str (:description item) " " (:url item))
     }))

;; (.start (Thread. #(consumer "notify")))
(defn- consumer
  [route]
  (mq/with-broker mq-config
    (mq/with-channel
      (mq/with-queue (str route ".queue")
        ;; (mq/queue-get true) ; consumes messages with auto-acknowledge enabled
        (println "start doseq..")

        (doseq [msg (mq/consuming-seq true)] ; consumes messages with auto-acknowledge enabled
          (println route ":received:" (String. (:body msg)))
          (let [message (String. (:body msg))
                {gtalk_id :gtalk description :description} (parse-notification message)]
            (if (or gtalk_id)
              (gtalk/send-message gtalk_id description)))
          )))))

(def *consumer* (atom nil))

(defn consume-notification
  []
  (if (nil? @*consumer*)
    (swap! *consumer* (fn[oldthread] (Thread. #(consumer "notify")))))
  (.start @*consumer*))

(defn stop-notification
  []
  (if (not (nil? @*consumer*))
    (.interrupt @*consumer*)))
     
