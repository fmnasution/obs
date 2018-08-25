(ns obs.validator.validator
  (:require
   [clojure.spec.alpha :as s]
   [bouncer.core :as bnc]))

;; ================================================================
;; validator spec
;; ================================================================

(s/def ::schema
  (s/map-of keyword? (s/or :fn fn?
                           :vec (s/cat :fn   fn?
                                       :args (s/* any?)))))

;; ================================================================
;; protocols spec
;; ================================================================

(s/def ::valid?-output
  boolean?)

(s/def ::validate-output
  (s/tuple (s/nilable map?)
           map?))

;; ================================================================
;; protocols
;; ================================================================

(defprotocol IValidator
  (-valid? [this data])
  (-validate [this data]))

;; ================================================================
;; validator
;; ================================================================

(defrecord Validator [schema]
  IValidator
  (-valid? [this data]
    (bnc/valid? data schema))
  (-validate [this data]
    (bnc/validate data schema)))

(defn make-validator
  [schema]
  (map->Validator {:schema (s/assert ::schema schema)}))

(defn valid?
  [validator data]
  (->> data
       (-valid? validator)
       (s/assert ::valid?-output)))

(defn invalid?
  [validator data]
  (not (valid? validator data)))

(defn validate
  [validator data]
  (->> data
       (-validate validator)
       (s/assert ::validate-output)))
