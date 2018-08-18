(set-env!
 :source-paths #{"src/"}
 :resource-paths #{"resources/"}
 :dependencies '[;; ---- clj ----
                 [org.clojure/clojure "1.10.0-alpha5"]
                 [aero "1.1.3"]
                 [http-kit "2.3.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [ring/ring-defaults "0.3.2"]
                 [metosin/muuntaja "0.5.0"]
                 [ring-cors "0.1.12"]
                 [liberator "0.15.2"]
                 [io.clojure/liberator-transit "0.3.1"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 [buddy/buddy-sign "3.0.0"]
                 [clj-time "0.14.4"]
                 ;; ---- cljc ----
                 [com.stuartsierra/component "0.3.2"]
                 [bidi "2.1.3"]
                 [mur "0.1.3-SNAPSHOT"]
                 [bouncer "1.0.1"]
                 [com.taoensso/encore "2.97.0"]])

(require
 '[mur.boot :refer [system]]
 '[obs.system :refer [dev-system-map]])

(deftask dev-repl
  []
  (comp
   (repl :server true)
   (watch)
   (system :system 'obs.system/dev-system-map
           :files  ["system.clj"
                    "endpoints.clj"
                    "config.edn"])))
