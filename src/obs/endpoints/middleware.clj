(ns obs.endpoints.middleware
  (:require
   [taoensso.encore :as u]
   [mur.components.middleware :as cptmdw]))

;; ================================================================
;; ring middleware
;; ================================================================

(defn- inject-closure
  [handler component]
  (fn [{:keys [body-params
              route-params
              user-credentials
              forget-claims
              reset-claims]
       :as   request}]
    (let [context (u/merge component
                           {:body-params      body-params
                            :route-params     route-params
                            :user-credentials user-credentials
                            :forget-claims    forget-claims
                            :reset-claims     reset-claims})]
      ((handler request) context))))

(defn make-ring-middleware
  []
  (cptmdw/make-middleware [[inject-closure :component]]))
