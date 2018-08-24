(ns obs.main.logger
  (:require
   [taoensso.timbre.appenders.core :refer [println-appender]]
   [mur.components.timbre :as cpttmb]))

;; ================================================================
;; logger
;; ================================================================

(defn make-println-logger
  [config]
  (-> config
      (dissoc :middleware :output-fn :appenders)
      (assoc :appenders {:println (println-appender)})
      (cpttmb/make-logger)))

(defn make-logger
  [config]
  (case (:kind config)
    :println (make-println-logger config)))
