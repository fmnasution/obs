(ns obs.endpoints.adapter)

(defn apply-context
  [context-handler]
  (fn [request]
    (fn [context & more-context]
      ((apply context-handler context more-context) request))))
