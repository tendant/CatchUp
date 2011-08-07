(ns CatchUp.couch
  (:require [com.ashafa.clutch :as couchdb]
            ))

;; couchdb

(def *couch-database*
  (couchdb/get-database {:name     "catchup_db"
                         ;;:language "clojure"
                         }))     ; use Couchdb (Clojure) view server

(defn save-item
  [document-map id]
  (couchdb/with-db *couch-database*
    (if (not (nil? id))
      (if (nil? (couchdb/get-document id))
        (do
          (println (str "INFO: Saving document: " id))
          (couchdb/create-document document-map id))
        (do
          (println (str "WARNING: id(" id ") does exist!"))
          ;; (couchdb/get-document id) ; FIXME: uncomment this line for testing.
          ;; (couchdb/update-document document-map id)
        )))))

(defn update-item
  [document]
  (couchdb/with-db *couch-database*
    (couchdb/update-document document)))

(defn list-all-items
  []
  (couchdb/with-db *couch-database*
    (couchdb/get-all-documents-meta)))

(defn get-item
  [id]
  ;; (pprint/pprint id)
  (couchdb/with-db *couch-database*
    (couchdb/get-document id)))

(defn delete-item
  [id]
  (couchdb/with-db *couch-database*
    (let [document (couchdb/get-document id)]
      (couchdb/delete-document document))))

;; Create user subscriptions view
(defn create-user-subscription-view
  []
  (couchdb/with-db *couch-database*
    (if (nil? (get-item "_design/user_subscription"))
      (save-item {
                  "_id" "_design/user_subscription",
                  "language" "javascript",
                  "views" {
                           "user_subscription" {
                                                "map" "function(doc) {\u000a  for (var i in doc.subscriptions) {\u0009\u0009\u000a    emit(i, (doc.subscriptions[i])[\"last-notification\"]);\u000a  }\u000a}"
                                                }
                           }
                  }
                 "_design/user_subscription")
      )))

;; find user subscriptions
;;
;; curl -X POST -d '{"keys":["news.ycombinator.com"]}' -H"Content-Type: application/json" http://localhost:5984/catchup_db/_design/user_subscription/_view/user_subscription
;;
;; Create View (in REPL)
;;
;; (save-item {
;;    "_id" "_design/user_subscription",
;;    "language" "javascript",
;;    "views" {
;;        "user_subscription" {
;;            "map" "function(doc) {\u000a  for (var i in doc.subscriptions) {\u0009\u0009\u000a    emit(i, (doc.subscriptions[i])[\"last-notification\"]);\u000a  }\u000a}"
;;        }
;;    }
;; }
;; "_design/user_subscription")
;; 
(defn find-subscribed-users
  "return all subscribed users for given source_id"
  [source_id]
  (couchdb/with-db *couch-database*
    (couchdb/get-view "user_subscription" "user_subscription" {} {:keys [source_id]})))

