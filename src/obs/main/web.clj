(ns obs.main.web
  (:require
   [mur.components.http-kit :as cpthkit]))

;; ================================================================
;; ring router
;; ================================================================

(defn make-web-server
  [config]
  (cpthkit/make-web-server config))
