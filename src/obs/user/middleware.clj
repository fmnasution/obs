(ns obs.user.middleware
  (:require
   [clojure.string :as str]
   [buddy.core.codecs :as bdycdc]
   [buddy.core.codecs.base64 :as bdycdc64]
   [mur.components.middleware :as cptmdw]
   [taoensso.encore :as u]
   [obs.user.signer :as usrsgn]))

;; =================================================================
;; header
;; =================================================================

(defn- get-header
  [request header-name]
  (some-> (:headers request)
          (as-> <> (first (filter
                           #(.equalsIgnoreCase header-name (name (key %)))
                           <>)))
          (val)))

(defn- get-token
  [header-value scheme]
  (let [scheme-pattern (re-pattern (str "^" scheme " (.*)$"))]
    (second (re-find scheme-pattern header-value))))

(defn- parse-authorization
  [request scheme]
  (when-let [header-value (get-header request "authorization")]
    (get-token header-value scheme)))

;; =================================================================
;; middleware
;; =================================================================

(defn- http-basic-token->credentials
  [token]
  (when-let [[username password] (u/catching
                                  (some-> token
                                          (bdycdc64/decode)
                                          (bdycdc/bytes->str)
                                          (str/split #":" 2))
                                  _
                                  ["" ""])]
    (if (and (some? username) (some? password))
      {:username username
       :password password}
      {})))

(defn -wrap-user-credentials
  [handler]
  (fn [request]
    (u/if-let [auth-token  (parse-authorization request "Basic")
               credentials (http-basic-token->credentials auth-token)]
      (handler (assoc request :user-credentials credentials))
      (handler request))))

(defn -wrap-forget-password-claims
  [handler {:keys [signer]}]
  (fn [request]
    (u/if-let [token  (parse-authorization request "ObsForget")
               claims (u/catching (usrsgn/unsign signer token) _ {})]
      (handler (assoc request :forget-claims claims))
      (handler request))))

(defn -wrap-reset-password-claims
  [handler {:keys [signer]}]
  (fn [request]
    (u/if-let [token  (parse-authorization request "ObsReset")
               claims (u/catching (usrsgn/unsign signer token) _ {})]
      (handler (assoc request :reset-claims claims))
      (handler request))))

;; =================================================================
;; ring middleware
;; =================================================================

(defn make-ring-middleware
  []
  (cptmdw/make-middleware [-wrap-user-credentials
                           [-wrap-forget-password-claims :component]
                           [-wrap-reset-password-claims :component]]))
