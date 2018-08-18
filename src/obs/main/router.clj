(ns obs.main.router
  (:require
   [ring.util.http-response :as resp]
   [mur.components.bidi :as cptbd]))

;; ================================================================
;; ring router
;; ================================================================

(defn make-ring-router
  []
  (cptbd/make-ring-router
   (constantly (resp/content-type
                (resp/not-found)
                "text/plain"))))
