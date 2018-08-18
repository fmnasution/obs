(ns obs.main.datastore.datomic
  (:require
   [mur.components.datomic :as cptdtm]))

;; ================================================================
;; datomic
;; ================================================================

(defn make-datomic-blueprint
  [config]
  (cptdtm/make-durable-datomic-db config))

(defn make-datomic-conn
  []
  (cptdtm/make-datomic-conn))
