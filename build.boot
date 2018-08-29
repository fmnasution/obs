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
                 [mur "0.1.9-SNAPSHOT"]
                 [bouncer "1.0.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.97.0"]
                 ;; ---- dev ----
                 [metosin/bat-test "0.4.0" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]])

(require
 '[mur.boot :refer [system]]
 '[metosin.bat-test :refer [bat-test]]
 ;; '[obs.app :refer [make-dev-system-map]]
 '[adzerk.bootlaces :refer [bootlaces! build-jar push-snapshot push-release]])

(def +project-name+
  'obs)

(def +version+
  "0.1.1-SNAPSHOT")

(bootlaces! +version+)

(task-options!
 push {:ensure-branch nil
       :repo-map      {:checksum :warn}}
 pom  {:project     +project-name+
       :version     +version+
       :description "An HTTP authentication server"
       :url         "http://github.com/fmnasution/obs"
       :scm         {:url "http://github.com/fmnasution/obs"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask dev-system-repl
  []
  (merge-env!
   :source-paths #{"env/dev/"})
  (require 'obs.app)
  (comp
   (repl :server true)
   (watch)
   (system :system 'obs.app/make-dev-system-map
           :files  ["app.clj"
                    "system.clj"
                    "config.edn"
                    "norm_map.edn"])
   (bat-test)))

(deftask dev-repl
  []
  (merge-env!
   :source-paths #{"env/dev/"})
  (comp
   (repl :server true)
   (watch)
   (system)
   (bat-test)))

(deftask build
  []
  (merge-env!
   :source-paths #{"env/prod/"})
  (require 'obs.app)
  (comp
   (aot :namespace #{'obs.app})
   (uber)
   (jar :file (str +project-name+ "-" +version+) :main 'obs.app)
   (sift :include #{#"obs.jar"})
   (target)))

(deftask snapshot-to-clojars
  []
  (comp
   (build-jar)
   (push-snapshot)))
