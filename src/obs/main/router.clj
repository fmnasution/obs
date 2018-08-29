(ns obs.main.router
  (:require
   [ring.util.http-response :as resp]
   [mur.components.ring :as cptrng]
   [mur.components.bidi :as cptbd]))

;; ================================================================
;; ring router
;; ================================================================

(defn make-ring-head
  []
  (cptrng/make-ring-head))

(defn make-route-collector
  []
  (cptbd/make-route-collector ""))

(defn make-ring-router
  []
  (cptbd/make-context-ring-router
   (constantly (resp/content-type
                (resp/not-found)
                "text/plain"))
   [:body-params
    :route-params
    :user-credentials
    :forget-claims
    :reset-claims]))
