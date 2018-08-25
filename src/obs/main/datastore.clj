(ns obs.main.datastore
  (:require
   [clojure.spec.alpha :as s]
   [obs.main.datastore.datomic :as mndtstdtm]))

;; ================================================================
;; datastore spec
;; ================================================================

(s/def ::kind
  #{:datomic})

(s/def ::config
  (s/keys :req-un [::kind]))

;; ================================================================
;; datastore
;; ================================================================

(defn make-datastore
  [config]
  (case (:kind (s/assert ::config config))
    :datomic (mndtstdtm/make-datomic-conn)))
