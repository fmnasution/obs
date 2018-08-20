(ns obs.endpoints.middleware
  (:require
   [taoensso.encore :as u]
   [mur.components.middleware :as cptmdw]))

;; ================================================================
;; ring middleware
;; ================================================================

(defn -inject-context
  [handler component ks]
  (fn [request]
    (let [context (u/merge component (select-keys request ks))]
      ((handler request) context))))

(defn make-ring-middleware
  []
  (cptmdw/make-middleware [[-inject-context
                            :component
                            [:body-params
                             :route-params
                             :user-credentials
                             :forget-claims
                             :reset-claims]]]))
