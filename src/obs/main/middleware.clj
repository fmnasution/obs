(ns obs.main.middleware
  (:require
   [clojure.string :as str]
   [ring.util.http-response :as resp]
   [ring.middleware.defaults :as mdwdef]
   [ring.middleware.cors :as mdwcrs]
   [muuntaja.middleware :as mdwmtj]
   [mur.components.middleware :as cptmdw]
   [taoensso.encore :as u]))

;; ================================================================
;; ring middleware
;; ================================================================

(defn -wrap-trailing-slash
  [handler]
  (fn [{:keys [uri] :as request}]
    (handler (assoc request :uri (if (and (not= "/" uri)
                                          (str/ends-with? uri "/"))
                                   (subs uri 0 (dec (count uri)))
                                   uri)))))

(defn -wrap-exception
  ([handler ex-handler]
   (fn [request]
     (u/catching
      (handler request)
      error
      (if (some? ex-handler)
        (ex-handler error request)
        (resp/internal-server-error)))))
  ([handler]
   (-wrap-exception handler nil)))

(defn -wrap-cors
  [handler config]
  (let [patterns (into [] (map re-pattern) (:allowed-origins config))]
    (mdwcrs/wrap-cors handler
                      :access-control-allow-origin  patterns
                      :access-control-allow-methods [:post :put :delete])))

(defn make-ring-middleware
  [config]
  (cptmdw/make-middleware [-wrap-trailing-slash
                           -wrap-exception
                           [mdwdef/wrap-defaults mdwdef/api-defaults]
                           mdwmtj/wrap-format-request
                           [-wrap-cors config]]))
