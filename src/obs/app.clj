(ns obs.app
  (:require
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [com.stuartsierra.component :as c]
   [aero.core :refer [read-config]]
   [taoensso.encore :as u]
   [obs.system :as sys])
  (:gen-class))

;; ================================================================
;; apps
;; ================================================================

(defn- load-config
  [source profile]
  (read-config (io/file source) {:profile profile}))

(defn make-dev-system-map
  []
  (sys/make-system-map (load-config "private/obs/config.edn" :dev)))

(def ^:private cli-spec
  [["-t" "--target TARGET" "Target path of the aero config"]
   ["-p" "--profile PROFILE" "Profile to use in the config"
    :parse-fn u/as-kw]
   ["-h" "--help" "Print this help"]])

(defn -main
  [& args]
  (let [{:keys [options arguments summary errors]}
        (parse-opts args cli-spec)
        {:keys [target profile]} options]
    (u/cond
      (nil? target)
      (throw (ex-info "No target specified" {}))

      (nil? profile)
      (throw (ex-info "No profile specified" {}))

      :let [config (load-config target profile)
            system (sys/make-system-map config)
            port   (get-in config [:web-server :port])]

      :do (println "Starting web server on port:" port)

      :else (c/start system))))
