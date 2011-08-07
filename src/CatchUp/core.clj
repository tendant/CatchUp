(ns CatchUp.core
  (:gen-class)
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as enlive]
            [clj-time.core :as time]
            [clojure.contrib.pprint :as pprint]
            [clojure.contrib.io :as io]
            [CatchUp.scheduler :as scheduler]
            [CatchUp.messager :as messager]
            [CatchUp.couch :as couch]
            [CatchUp.filters :as filters]
            [cheshire.core :as json]
            ))

(def source_type ["URL" "FILE"])

(def source_map {
                 "news.ycombinator.com" {
                                         :location "http://news.ycombinator.com/news"
                                         :type :URL
                                         :parser 'CatchUp.core/yc-news-parser
                                         :selector 'CatchUp.core/yc-news-selector
                                         :next-page-selector 'CatchUp.core/yc-news-next-page-selector
                                         ;; :total-pages 1 ;; only support first page for now.
                                         }
                 "data/hacker_news.html" {
                                          :location "data/hacker_news.html"
                                          :type :FILE
                                          :parser 'CatchUp.core/yc-news-parser
                                          :selector 'CatchUp.core/yc-news-selector
                                          }
                                          
                })

(defn get-html-resource-location
  [location type]
  (cond
   (= :URL type) (java.net.URL. location)
   (= :FILE type) location
   :else (println "Error: Not supported resource type:" type)))
  
(defn get-processor
  [source-id processor-type]
  (let [selector-str (get-in source_map [source-id processor-type])]
    (if (nil? selector-str)
      (println "ERROR: cannot find '"
               processor-type "' for : '"  source-id "'")
      (let [selector (resolve selector-str)]
        (if (nil? selector)
          (println "ERROR: Cannot resolve '"
                   selector-str "' for: '" processor-type "'")
          selector)))))

(defn get-parser
  [source-id]
  (get-processor source-id :parser))

(defn get-selector
  [source-id]
  (get-processor source-id :selector))

(defn get-next-page-selector
  [source-id]
  (get-processor source-id :next-page-selector))

(defn get-next-page-location
  [source-id page]
  (when-let [next-page-selector (get-next-page-selector source-id)]
    (next-page-selector page)))

;; news.ycombinator.com
(defn yc-news-valid?
  "filter out incorrect selection"
  [item]
  (= (count (enlive/select item [:tr])) 2))

(defn yc-news-selector
  "select yc news from fetched page"
  [page]
  (->> 
   (enlive/select page {[:tr (enlive/has [:td.title])] [:tr (enlive/has [:td.subtext])]})
   (filter yc-news-valid?)
   ))

(defn yc-news-parser
  "parse yc news from items"
  [item source-id]
  (let [desciption (enlive/text (first (enlive/select item [:td.title :a])))
        url (:href (:attrs (first (enlive/select item [:td.title :a]))))
        expired? false
        submit-by (enlive/text (first (enlive/select item [:td.subtext :a])))
        ]
    (hash-map :description desciption
              :url (if (.startsWith url "item?")
                     (str "http://news.ycombinator.com/" url)
                     url)
              :source source-id
              :expired? expired?
              :submit-by submit-by
              :created-on (.toString (time/now)))))

;; enlive

(defn fetch-web-page
  "fetch web page from url"
  [location type]
  (try
    (enlive/html-resource (get-html-resource-location location type))
    (catch Exception e
      (println (str "ERROR: fetch page failed! Location:" location "Type:" type e)))))

(defn parse-all-items
  "process html page and parse all items!"
  [source-id page]
  (println (.toString (time/now)) "parse-all-items" source-id)
  (when-let [selector (get-selector source-id)]
    (when-let [parser (get-parser source-id)]
      (map #(parser %1 source-id)
           (selector page)))))

(defn fetch-source
  "fetch multiple pages from website"
  [source-id]
  (let [source (get source_map source-id)
        total-pages (:total-pages source 1)
        ]
    (loop [location (:location source)
           type (:type source)
           current-page 0
           items nil
           ]
      (if (and (< current-page total-pages)
               (not (nil? location)))
        (let [html-page (fetch-web-page location type)]
          (recur (get-next-page-location source-id html-page)
                 (:type source)
                 (inc current-page)
                 (reduce #(assoc %1 (:url %2) %2)
                         items
                         (parse-all-items source-id html-page))))
        items))))

;; USER

(defrecord User [id alias fname lname email gtalk])

;; example: (create-user (User. "tendant" "neil" "Lei" "W" "tendant@gmail.com" "tendant@gmail.com"))
(defn create-user
  "create a new user"
  [user]
  (hash-map :user user :type "USER"))

;; CHANGE-ME
(defn create-test-user
  "create a test user"
  []
  (let [id "tendant"
        user (assoc
                 (create-user (User. id "neil" "Lei" "W"
                                     "tendant@gmail.com"
                                     "tendant@gmail.com"))
               "subscriptions" {"news.ycombinator.com"
                                {"filters" {"filter1" ["google"]
                                            "filter2" ["facebook" "api"]
                                            "filter3" ["yc"]
                                            }
                                 }
                                }
               )]
    (if (nil? (couch/get-item id))
      (couch/save-item user id)
      (println "INFO: user does exist: " id)
      )))

;; example :
;;
;; (update-user-subscribe (couch/get-item "test1-id") "news.ycombinator.com" {:filter1 ["google" "api"] :filter2 ["facebook"]})
;; 
;; (couch/update-item (update-user-subscribe (couch/get-item "tendant") "news.ycombinator.com" {:filter1 ["google" "api"] :filter2 ["facebook"]}))
(defn update-user-subscribe
  "add subscription for user"
  [user source_id filters]
  (let [subscriptions (:subscriptions user)]
    (if (nil? user)
      (println "Cannot find user: " user)
      (assoc user :subscriptions
             (assoc subscriptions
               (keyword source_id)
               (hash-map :last-notification (.toString (time/now))
                         :filters filters)
               )))))



;; couchdb & enlive

(defn save-parsed-item
  "save item into couchdb"
  [item]
  (couch/save-item item (:url item)))


(defn prepare-for-notification
  [user_id item_id]
  (println "prepare for:" user_id item_id)
  (json/generate-string
   {:user_id user_id
    :item_id item_id}))

(defn publish-notifications
  [messages]
  (doseq [message messages]
    (println "queueing message: " message)
    (messager/publisher "notify" message)))

(defn item-subscription
  "Find subscribed user for given item, and publish it for notification"
  [item]
  (println "item-subscription" (:_id item))
  (if (not (nil? item))
    (let [source_id (:source item)
          subscriptions (:rows (couch/find-subscribed-users source_id))]
      (->> subscriptions
           (filter #(filters/user-filter-match? (couch/get-item (:id %1)) item))
           (map #(prepare-for-notification (:id %1) (:url item)))
           (publish-notifications)))))

(defn process-parsed-item
  "save parsed item, and publish it"
  [item]
  (when-let [saved-item (save-parsed-item item)]
    (println "process-parsed-item" (:_id saved-item))
    (item-subscription saved-item)
    ;; (pprint/pprint saved-item)
    ))

(defn yc-news-worker
  []
  (scheduler/periodically (fn[] (doseq [item (parse-all-items "news.ycombinator.com" (fetch-web-page "http://news.ycombinator.com/news" :URL))]
                                  (process-parsed-item item)))
                          1000 100000))

(defn -main
  []
  (create-test-user)
  (couch/create-user-subscription-view)
  (messager/broker)
  (messager/consume-notification)
  (yc-news-worker)
  )
  