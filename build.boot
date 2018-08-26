(set-env!
 :source-paths #{"src/" "test/"}
 :resource-paths #{"resources/"}
 :dependencies '[;; ---- clj ----
                 [org.clojure/clojure "1.10.0-alpha5"]
                 [aero "1.1.3"]
                 [http-kit "2.3.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [com.datomic/datomic-free "0.9.5697"]
                 [io.rkn/conformity "0.5.1"]
                 [ring/ring-defaults "0.3.2"]
                 [metosin/muuntaja "0.5.0"]
                 [ring-cors "0.1.12"]
                 [liberator "0.15.2"]
                 [io.clojure/liberator-transit "0.3.1"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 [buddy/buddy-sign "3.0.0"]
                 [clj-time "0.14.4"]
                 [ring-logger "1.0.1"]
                 [org.clojure/tools.cli "0.3.7"]
                 ;; ---- cljc ----
                 [com.stuartsierra/component "0.3.2"]
                 [bidi "2.1.3"]
                 [mur "0.1.6-SNAPSHOT"]
                 [bouncer "1.0.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.97.0"]
                 ;; ---- dev ----
                 [metosin/bat-test "0.4.0" :scope "test"]])

(require
 '[mur.boot :refer [system]]
 '[metosin.bat-test :refer [bat-test]]
 '[obs.app :refer [make-dev-system-map]])

(deftask dev-system-repl
  []
  (comp
   (repl :server true)
   (watch)
   (system :system 'obs.app/make-dev-system-map
           :files  ["system.clj"
                    "endpoints.clj"
                    "config.edn"
                    "norm_map.edn"])
   (bat-test)))

(deftask dev-repl
  []
  (comp
   (repl :server true)
   (watch)
   (system)
   (bat-test)))

(deftask build
  []
  (comp
   (aot :namespace #{'obs.app})
   (uber)
   (jar :file "obs.jar" :main 'obs.app)
   (sift :include #{#"obs.jar"})
   (target)))
