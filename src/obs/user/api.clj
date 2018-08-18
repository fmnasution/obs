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
  (create-and-return [this user])
  (authenticate [this user-credentials]))

;; ================================================================
;; implementing protocols
;; ================================================================

(defn- datomic->token-ready
  [user]
  (-> user
      (select-keys [:db.entity/id :user/username])
      (set/rename-keys {:db.entity/id  :id
                        :user/username :username})))

(defn- password-ok?
  [attempt encrypted]
  (u/catching (bdyhsh/check attempt encrypted) _ false))

(extend-protocol IUserAPI
  mur.components.datomic.DatomicConn
  (get-by-username [{:keys [db]} username]
    (datomic->token-ready (dtm/entity db [:user/username username])))
  (update-password-by-username [{:keys [conn]} username password]
    )
  (delete-by-username [this username]
    )
  (create-and-return [{:keys [conn]} {:keys [username password]}]
    (let [eid                        (dtm/tempid :db.part/user)
          tx-data                    [{:db/id         eid
                                       :user/username username
                                       :user/password password}]
          {:keys [db-after tempids]} @(dtm/transact conn tx-data)]
      (datomic->token-ready
       (dtm/entity db-after (dtm/resolve-tempid db-after tempids eid)))))
  (authenticate [{:keys [db]} {:keys [username password]}]
    (u/when-let [user   (dtm/entity db [:user/username username])
                 match? (password-ok? password (:user/password user))]
      (datomic->token-ready user))))
