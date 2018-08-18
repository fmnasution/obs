(ns obs.endpoints.decisions
  (:require
   [clojure.string :as str]
   [buddy.hashers :as bdyhsh]
   [taoensso.encore :as u]
   [obs.user.api :as usrapi]))

;; ================================================================
;; helpers
;; ================================================================

(defn run-on-request-method
  [f request-methods default-value]
  (fn [{:keys [request] :as ctx}]
    (if (some #{(:request-method request)} request-methods)
      (f ctx)
      default-value)))

;; ================================================================
;; authorized?
;; ================================================================

(defn check-authenticated
  [datastore user-credentials]
  (when-let [user (usrapi/authenticate datastore user-credentials)]
    {:self user}))

;; ================================================================
;; malformed?
;; ================================================================

(defn no-content-type?
  [{:keys [request] :as ctx}]
  (nil? (:muuntaja/format request)))

;; ================================================================
;; known content type
;; ================================================================

(defn- content-type
  [ctx]
  (when-let [media-type (get-in ctx [:request :headers "content-type"])]
    (-> media-type
        (str/split #"\s*;\s*")
        (first))))

(defn supported-content-type?
  [{:keys [request] :as ctx} content-types]
  (u/cond
    (not (#{:put :post :patch} (:request-method request)))
    true

    :let [current-content-type (content-type ctx)]

    (nil? current-content-type)
    true

    :else (some #{current-content-type} content-types)))

