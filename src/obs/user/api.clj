(ns obs.user.api
  (:require
   [clojure.set :as set]
   [datomic.api :as dtm]
   [buddy.hashers :as bdyhsh]
   [mur.components.datomic :as cptdtm]
   [taoensso.encore :as u]))

;; ================================================================
;; protocols
;; ================================================================

(defprotocol IUserAPI
  (get-by-username [this username])
  (update-password-by-username [this username password])
  (delete-by-username [this username])
  (create-and-return [this user]))

;; ================================================================
;; implementing protocols
;; ================================================================

(defn- datomic->token-ready
  [user]
  (some-> user
          (select-keys [:db.entity/id :user/username :user/password])
          (set/rename-keys {:db.entity/id  :id
                            :user/username :username
                            :user/password :password})))

(defn- password-ok?
  [attempt encrypted]
  (u/catching (bdyhsh/check attempt encrypted) _ false))

(extend-protocol IUserAPI
  mur.components.datomic.DatomicConn
  (get-by-username [{:keys [conn db]} username]
    (let [db (or db (dtm/db conn))]
      (datomic->token-ready (dtm/entity db [:user/username username]))))
  (update-password-by-username [{:keys [conn]} username password]
    (let [tx-data [{:db/id         [:user/username username]
                    :user/password (bdyhsh/derive password)}]]
      @(dtm/transact conn tx-data)))
  (delete-by-username [{:keys [conn]} username]
    @(dtm/transact conn [[:db.fn/retractEntity [:user/username username]]]))
  (create-and-return [{:keys [conn]} {:keys [username password]}]
    (let [eid     (dtm/tempid :db.part/user)
          tx-data [{:db/id         eid
                    :user/username username
                    :user/password (bdyhsh/derive password)}]

          {:keys [db-after tempids]}
          @(dtm/transact conn tx-data)]
      (datomic->token-ready
       (dtm/entity db-after (dtm/resolve-tempid db-after tempids eid))))))

(defn authenticate
  [datastore {:keys [username password]}]
  (u/when-let [user   (get-by-username datastore username)
               match? (password-ok? password (:password user))]
    user))
