(ns obs.test.mock
  (:require
   [buddy.hashers :as bdyhsh]
   [obs.user.signer :as usrsgn]
   [obs.user.api :as usrapi]
   [obs.validator.validator :as vldtvldt]))

;; ================================================================
;; mock datastore
;; ================================================================

(defn- generate-id
  [user-map]
  (inc (last (sort (map :id (vals user-map))))))

(defn make-mock-datastore
  [users]
  (let [state_ (atom (into {}
                           (map-indexed
                            (fn [idx {:keys [username] :as user}]
                              [username
                               (-> user
                                   (update :password bdyhsh/derive)
                                   (assoc :id idx))]))
                           users))]
    (reify usrapi/IUserAPI
      (get-by-username [_ username]
        (get @state_ username))
      (update-password-by-username [_ username password]
        (when (contains? @state_ username)
          (swap! state_
                 assoc-in
                 [username :password]
                 (bdyhsh/derive password))))
      (delete-by-username [_ username]
        (swap! state_ dissoc username))
      (create-and-return [_ {:keys [username] :as user}]
        (let [new-user (-> user
                           (update :password bdyhsh/derive)
                           (assoc :id (generate-id @state_)))
              result   (swap! state_ assoc username new-user)]
          (get @state_ username))))))

;; ================================================================
;; mock signer
;; ================================================================

(defn make-mock-signer
  []
  (reify usrsgn/ISigner
    (sign [_ data]
      (str data "!"))
    (unsign [_ signed-data]
      (subs signed-data 0 (dec (count signed-data))))))

;; ================================================================
;; mock validator
;; ================================================================

(defn make-mock-validator
  [success?]
  (reify vldtvldt/IValidator
    (valid? [_ data]
      success?)
    (validate [_ data]
      [(if success? {} {:error? true}) data])))
