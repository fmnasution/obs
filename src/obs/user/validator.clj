(ns obs.user.validator
  (:require
   [clojure.string :as str]
   [bouncer.validators :as bncv :refer [defvalidator]]
   [mur.components.bouncer :as cptbnc]))

;; =================================================================
;; predicate
;; =================================================================

(defvalidator no-colon
  {:default-message-format "%s shouldn't contain a colon"}
  [v]
  (not (str/includes? v ":")))

;; ================================================================
;; create user validator
;; ================================================================

(defn create-user-schema
  []
  {:username [bncv/required
              bncv/string
              [bncv/max-count 20]
              no-colon]
   :password [bncv/required
              bncv/string]})

(defn make-create-user-validator
  []
  (cptbnc/make-validator (create-user-schema)))

;; ================================================================
;; user credentials validator
;; ================================================================

(defn user-credentials-schema
  []
  {:username [bncv/required
              bncv/string]
   :password [bncv/required
              bncv/string]})

(defn make-user-credentials-validator
  []
  (cptbnc/make-validator (user-credentials-schema)))

;; ================================================================
;; forget claims validator
;; ================================================================

(defn forget-claims-schema
  []
  {:username [bncv/required
              bncv/string]})

(defn make-forget-claims-validator
  []
  (cptbnc/make-validator (forget-claims-schema)))

;; ================================================================
;; reset claims validator
;; ================================================================

(defn reset-claims-schema
  []
  {:id       [bncv/required
              bncv/string]
   :username [bncv/required
              bncv/string]})

(defn make-reset-claims-validator
  []
  (cptbnc/make-validator (reset-claims-schema)))

;; ================================================================
;; update password validator
;; ================================================================

(defn update-password-schema
  []
  {:password [bncv/required
              bncv/string]})

(defn make-update-password-validator
  []
  (cptbnc/make-validator (update-password-schema)))
