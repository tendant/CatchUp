(ns CatchUp.gtalk)

;; gtalk configuration
(def *gtalk-connection* (atom nil))

;; CHANGE-ME
(def gtalk-user {:id "thisismygtalkid@gmail.com"
                 :password "thisismypassword"})

(defn- open-connection
  [connection]
  (if (or (nil? connection) (not (.isAuthenticated connection)))
    (let [conf (org.jivesoftware.smack.ConnectionConfiguration. "talk.google.com" 5222 "gmail.com")
          con (org.jivesoftware.smack.XMPPConnection. conf)]
      (.connect con)
      (org.jivesoftware.smack.SASLAuthentication/supportSASLMechanism "PLAIN" 0)
      (.login con (:id gtalk-user) (:password gtalk-user) "resource")
      (if (.isAuthenticated con)  ;; check that we are authenticated
        con
        (println "ERROR: Cannot connect to gtalk server")))
    connection ;; reuse existing connection.
    ))

(defn get-connection
  []
  (let [conn @*gtalk-connection*]
    (if (or (nil? conn) (not (.isAuthenticated conn)))
      (swap! *gtalk-connection* open-connection)
      conn)))

(defn- disconnec-connection
  [connection]
  (if (not (nil? connection))
    (.disconnect connection)))

(defn close-connection
  []
  (let [conn @*gtalk-connection*]
    (if (not (nil? conn))
      (swap! *gtalk-connection* disconnec-connection))))

(defn send-message
  [gtalk-id message]
  (let [conn (get-connection)
        roster (.getRoster conn)
        presence (.getPresence roster gtalk-id)
        type (.getType presence)
        mode (.getMode presence)
        chat (.createChat (.getChatManager conn) gtalk-id nil)]
    (println gtalk-id)
    (println type)
    (println mode)
    (if (= type org.jivesoftware.smack.packet.Presence$Type/unavailable)
      (do (println "unavailable")
          (.createEntry roster gtalk-id gtalk-id nil)))
    (if (or (not (= (.getMode presence) org.jivesoftware.smack.packet.Presence$Mode/dnd))
            (nil? (.getMode presence)))
      (do (println "mode: " mode "message: " message)
          (println "sending...")
          (.sendMessage chat message))
      (println "mode: " mode " message: " message))))
    
