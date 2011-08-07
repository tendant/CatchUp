(defproject CatchUp "1.0.0-SNAPSHOT"
  :description "FIXME: write"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [clj-http "0.1.3"]
                 [clj-time "0.3.0"]
                 [enlive "1.0.0"]
                 [com.ashafa/clutch "0.2.4"] ;; couchdb library
                 [com.mefesto/wabbitmq "0.1.4"]
                 [cheshire "1.1.4"] ;; json library
                 [org.igniterealtime.smack/smack "3.2.1"]
                 [org.igniterealtime.smack/smackx "3.2.1"]
                 ]
  :dev-dependencies [[swank-clojure "1.3.0"]
                     ;; [com.stuartsierra/lazytest "2.0.0-SNAPSHOT"]
                     ]
  ;; :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"
  ;;                "stuartsierra-snapshots" "http://stuartsierra.com/m2snapshots"}
  :main CatchUp.core
  )
