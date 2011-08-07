(ns CatchUp.test.filters
  (:use [CatchUp.filters] :reload)
  (:use [clojure.test])
  (:use [CatchUp.core]))

(def hacker_news "data/hacker_news.html")

(def hacker-news-filters
  {:filter1 ["html5"]
   :filter2 ["yahoo" "facebook"]})

(deftest test-single-keyword-match?
  (is (keyword-match?
       "yahoo"
       "yahoo"))
  (is (keyword-match?
       "yahoo"
       "Yahoo's assumptions in 2006 about Facebook's future"))
  (is (keyword-match?
       "facebook"
       "Yahoo's assumptions in 2006 about Facebook's future"))
  )

(deftest test-keyword-match?
  (is (keyword-match?
       "tactics"
       "One of the most important tactics to understand as a search engine marketer is how keyword match types work.")))

(deftest test-news-keyword-match?
  (is (keyword-match?
       "google"
       "A Brief Explanation of Microsoft's Anti-Google Patent FUD")))

(deftest test-keywords-match?
  (is (keywords-match?
       ["tactics" "engine"]
       "One of the most important tactics to understand as a search engine marketer is how keyword match types work.")))

(deftest test-news-keywords-match?
  (let [content "Yahoo's assumptions in 2006 about Facebook's future"]
    (is (keywords-match?
         ["facebook" "yahoo"]
         content))
    (is (keywords-match?
         ["facebook" "google"]
         content))
    (is (not  (keywords-match?
               ["twitter" "google"]
               content)))
         ))

(deftest test-news-all-keywords-match?
  (let [content "Yahoo's assumptions in 2006 about Facebook's future"]
    (is (all-keywords-match?
         ["facebook" "yahoo"]
         content))
    (is (not (all-keywords-match?
              ["facebook" "google"]
              content)))
    (is (not  (all-keywords-match?
               ["twitter" "google"]
               content)))
    ))

(deftest test-filter-news-by-keywords
  (is (=
       (count (filter-items-by-keywords ["facebook" "yahoo"] (parse-all-items hacker_news (fetch-web-page hacker_news :FILE))))
       2)))

(def test-news
  {:created-on "2011-07-23T19:40:32.346Z",
   :url "http://rawkes.com/blog/2011/08/06/browserscene-creating-a-3d-sound-visualiser-with-webgl-and-html5-audio"
   :expired? false,
   :source hacker_news
   :description
   "Creating a 3D sound visualiser with WebGL and the HTML5 Audio Data API"})

(def testuser (CatchUp.core.User. "test1-id" "test1-alias" "ftest" "ltest" "tendant@gmail.com" "tendant@gmail.com"))

(def test-hacker-user
  (update-user-subscribe (create-user testuser) hacker_news hacker-news-filters)
  )

(deftest test-hacker-user-filter-match?
  (is (get-in test-hacker-user [:subscriptions (keyword hacker_news) :filters]))
  (is (= (user-filter-match? test-hacker-user test-news)
         true)))
