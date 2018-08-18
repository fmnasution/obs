(ns obs.validator.validator
  (:require
   [bouncer.core :as bnc]))

;; ================================================================
;; protocols
;; ================================================================

(defprotocol IValidator
  (valid? [this data])
  (validate [this data]))

;; ================================================================
;; validator
;; ================================================================

(defrecord Validator [schema]
  IValidator
  (valid? [this data]
    (bnc/valid? data schema))
  (validate [this data]
    (bnc/validate data schema)))

(defn make-validator
  [schema]
  (map->Validator {:schema schema}))

(defn invalid?
  [validator data]
  (not (valid? validator data)))
