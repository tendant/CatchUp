(ns CatchUp.test.core
  (:use [CatchUp.core] :reload)
  (:use [clojure.test])
  (:require [clj-http.client :as http]
            [net.cgrand.enlive-html :as enlive]
            [clojure.contrib.pprint :as pprint]
            [clojure.contrib.io :as io]))

;; crawl page
;; parse page
;; get all items
;; 

(def hacker_news "data/hacker_news.html")

(deftest test-get-html-resource-location-file
  (let [result (get-html-resource-location hacker_news :FILE)]
    (is (= (type result)
           java.lang.String))))

(deftest test-hacker-news-item-selector
  (let [all-items (yc-news-selector (fetch-web-page hacker_news :FILE))]
    (is (=
         (count all-items)
         30))
    ))

(deftest test-parse-hacker-news
  (->>
   (yc-news-selector (fetch-web-page hacker_news :FILE))
   (map #(yc-news-parser % hacker_news))
   (every? #(not (nil? (:description %))))
   (is)
   ))

(deftest test-parse-all-hacker-news
  (is (=
       (count (parse-all-items hacker_news (fetch-web-page hacker_news :FILE)))
       30)))

(def testuser (CatchUp.core.User. "test1-id" "test1-alias" "ftest" "ltest" "tendant@gmail.com" "tendant@gmail.com"))

(def test-filters
  {:filter1 ["google"]
   :filter2 ["facebook"]
   :filter3 ["clojure"]})

(deftest test-create-user
  (let [user (create-user testuser)]
    (is (= (:type user)
           "USER"))
    (is (= (:id (:user user))
           "test1-id"))))
        
(deftest test-update-user-subscribe
  (let [user (create-user testuser)
        updated-user (update-user-subscribe user :test_item test-filters)]
    (is (nil? (:subscriptions user)))
    (is (not (nil? (:subscriptions updated-user))))
    (is (not (nil? (:test_item (:subscriptions updated-user)))))
    (is (= (type (:filters (:test_item (:subscriptions updated-user))))
           clojure.lang.PersistentArrayMap))
    (is (= (type (first (:filters (:test_item (:subscriptions updated-user)))))
           clojure.lang.MapEntry))
    (is (= (type (val(first (:filters (:test_item (:subscriptions updated-user))))))
           clojure.lang.PersistentVector))
    ))
  
;; prepare test data.
;; (io/spit "test/data/hacker_news.html"
;;                       (:body (http/get  "http://news.ycombinator.com")))
