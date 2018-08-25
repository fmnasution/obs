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
  [signer user hours-duration]
  {:auth-token (usrsgn/auth-token signer user hours-duration)})

(defn- present-reset-token
  [signer user hours-duration]
  {:reset-token (usrsgn/reset-token signer user hours-duration)})

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
  [{:keys [config
           datastore
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
  :handle-created #(present-auth-token signer
                                       (::result %)
                                       (:auth-token-duration config)))

(defn make-create-user-endpoint
  []
  (cptbd/make-ring-endpoint
   ["/user" (edpadpt/context-adapter create-user-endpoint)]))

;; ================================================================
;; reset token
;; ================================================================

(defresource reset-token-endpoint
  [{:keys [config
           datastore
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
                 (present-reset-token signer
                                      user
                                      (:reset-token-duration config))
                 (lrep/ring-response (resp/not-found)))))

(defn make-reset-token-endpoint
  []
  (cptbd/make-ring-endpoint
   [["/user/" :username "/reset"]
    (edpadpt/context-adapter reset-token-endpoint)]))

;; ================================================================
;; target user
;; ================================================================

(defresource target-user-endpoint
  [{:keys [config
           datastore
           signer
           body-params
           update-password-validator
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
                  :put    (fn [ctx]
                            (or (edpdcs/no-content-type? ctx)
                                (vldtvldt/invalid? update-password-validator
                                                   body-params)
                                (if (some? reset-claims)
                                  (vldtvldt/invalid? reset-claims-validator
                                                     reset-claims)
                                  (invalid-user-credentials?))))
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
  :allowed? (fn [_]
              (equal-username? route-params (if (some? reset-claims)
                                              reset-claims
                                              user-credentials)))
  :handle-forbidden (constantly (lrep/ring-response (resp/conflict)))
  :known-content-type? (l/by-method
                        {:post   true
                         :put    #(edpdcs/supported-content-type?
                                   %
                                   supported-media-types)
                         :delete true})
  :new? false
  :put! (fn [_]
          {::result (update-password-by-username datastore
                                                 route-params
                                                 body-params)})
  :delete! (fn [_]
             {::result (delete-by-username datastore route-params)})
  :respond-with-entity? (l/by-method {:post  true
                                      :put   true
                                      :delete false})
  :handle-ok (fn [{:keys [self]}]
               (present-auth-token signer
                                   self
                                   (:auth-token-duration config))))

(defn make-target-user-endpoint
  []
  (cptbd/make-ring-endpoint [["/user/" :username]
                             (edpadpt/context-adapter target-user-endpoint)]))
