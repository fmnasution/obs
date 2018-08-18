(ns obs.main.datastore
  (:require
   [obs.main.datastore.datomic :as mndtstdtm]))

;; ================================================================
;; datastore
;; ================================================================

(defn make-datastore
  [config]
  (case (:kind config)
    :datomic (mndtstdtm/make-datomic-conn)))
