(ns obs.endpoints.middleware
  (:require
   [taoensso.encore :as u]
   [mur.components.middleware :as cptmdw]))

;; ================================================================
;; ring middleware
;; ================================================================

(defn -inject-context
  [handler config component ks]
  (fn [request]
    (let [context (u/merge component
                           (select-keys request ks)
                           {:config config})]
      ((handler request) context))))

(defn make-ring-middleware
  [config]
  (cptmdw/make-middleware [[-inject-context
                            config
                            :component
                            [:body-params
                             :route-params
                             :user-credentials
                             :forget-claims
                             :reset-claims]]]))
