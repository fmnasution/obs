(ns obs.endpoints.adapter
  (:require
   [taoensso.encore :as u]))

(defn apply-context
  [context-handler]
  (fn [{:keys [route-params] :as request}]
    (fn [context & more-context]
      ((apply context-handler
              (u/assoc-when context :route-params route-params)
              more-context)
       request))))
