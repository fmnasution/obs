(ns obs.main.logger
  (:require
   [clojure.spec.alpha :as s]
   [taoensso.timbre.appenders.core :refer [println-appender]]
   [mur.components.timbre :as cpttmb]))

;; ================================================================
;; logger spec
;; ================================================================

(s/def ::kind
  #{:println})

(s/def ::config
  (s/keys :req-un [::kind]))

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
  (case (:kind (s/assert ::config config))
    :println (make-println-logger config)))
