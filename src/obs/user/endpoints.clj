(ns obs.user.endpoints
  (:require
   [ring.util.http-response :as resp]
   [liberator.core :as l :refer [defresource]]
   [liberator.representation :as lrep]
   [mur.components.bidi :as cptbd]
   [obs.endpoints.adapter :as edpadpt]
   [obs.endpoints.decisions :as edpdcs]
   [obs.validator.validator :as vldtvldt]
   [obs.user.api :as usrapi]
   [obs.user.signer :as usrsgn]))

;; ================================================================
;; helpers
;; ================================================================

(defn- present-auth-token
  [signer user]
  {:auth-token (usrsgn/auth-token signer user)})

(defn- present-reset-token
  [signer user]
  {:reset-token (usrsgn/reset-token signer user)})

(defn- get-by-username
  [datastore {:keys [username]}]
  (usrapi/get-by-username datastore username))

(defn- delete-by-username
  [datastore {:keys [username]}]
  (usrapi/delete-by-username datastore username))

(defn- update-password-by-username
  [datastore {:keys [username]} {:keys [password]}]
  (usrapi/update-password-by-username datastore username password))

(defn equal-username?
  [m1 m2]
  (= (:username m1) (:username m2)))

;; ================================================================
;; create user
;; ================================================================

(def ^:private supported-media-types
  ["application/json" "application/transit+json"])

(defresource create-user-endpoint
  [{:keys [datastore
           signer
           create-user-validator
           body-params]}]
  :allowed-methods [:post]
  :available-media-types supported-media-types
  :malformed? edpdcs/no-content-type?
  :known-content-type? #(edpdcs/supported-content-type? % supported-media-types)
  :processable? (fn [_]
                  (vldtvldt/valid? create-user-validator body-params))
  :conflict? (fn [_]
               (some? (get-by-username datastore body-params)))
  :post! (fn [_]
           {::result (usrapi/create-and-return datastore body-params)})
  :new? (comp nil? ::result)
  :handle-created #(present-auth-token signer (::result %)))

(defn make-create-user-endpoint
  []
  (cptbd/make-ring-endpoint
   ["/user" (edpadpt/apply-context create-user-endpoint)]))

;; ================================================================
;; reset token
;; ================================================================

(defresource reset-token-endpoint
  [{:keys [datastore
           signer
           route-params
           forget-claims
           forget-claims-validator]}]
  :available-media-types supported-media-types
  :allowed-methods [:post]
  :malformed? (fn [_]
                (vldtvldt/invalid? forget-claims-validator forget-claims))
  :conflict? (fn [_]
               (not (equal-username? route-params forget-claims)))
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [_]
               (if-let [user (get-by-username datastore forget-claims)]
                 (present-reset-token signer user)
                 (lrep/ring-response (resp/not-found)))))

(defn make-reset-token-endpoint
  []
  (cptbd/make-ring-endpoint
   [["/user/" :username "/reset"]
    (edpadpt/apply-context reset-token-endpoint)]))

;; ================================================================
;; target user
;; ================================================================

(defresource target-user-endpoint
  [{:keys [datastore
           signer
           body-params
           route-params
           user-credentials
           user-credentials-validator
           reset-claims
           reset-claims-validator]}]
  :available-media-types supported-media-types
  :allowed-methods [:post :put :delete]
  :malformed? (let [invalid-user-credentials?
                    #(vldtvldt/invalid? user-credentials-validator
                                        user-credentials)]
                (l/by-method
                 {:post   (fn [_]
                            (invalid-user-credentials?))
                  :put    (fn [_]
                            (if (some? reset-claims)
                              (vldtvldt/invalid? reset-claims-validator
                                                 reset-claims)
                              (invalid-user-credentials?)))
                  :delete (fn [_]
                            (invalid-user-credentials?))}))
  :authorized? (let [check-authenticated
                     #(edpdcs/check-authenticated datastore user-credentials)]
                 (l/by-method
                  {:post   (fn [_]
                             (check-authenticated))
                   :put    (fn [_]
                             (if (some? reset-claims)
                               true
                               (check-authenticated)))
                   :delete (fn [_]
                             (check-authenticated))}))
  :allowed? (fn [{:keys [self]}]
              (or (equal-username? route-params self)
                  (equal-username? route-params reset-claims)))
  :handle-forbidden (constantly (lrep/ring-response (resp/conflict)))
  :new? false
  :put! (fn [_]
          {::result (update-password-by-username datastore
                                                 route-params
                                                 body-params)})
  :delete! (fn [_]
             {::result (delete-by-username datastore route-params)})
  :respond-with-entity? (l/by-method {:post  false
                                      :put   true
                                      :delte false})
  :handle-ok (fn [{:keys [self]}]
               (present-auth-token signer self)))

(defn make-target-user-endpoint
  []
  (cptbd/make-ring-endpoint [["/user/" :username]
                             (edpadpt/apply-context target-user-endpoint)]))
