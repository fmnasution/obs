(ns obs.user.bootstrap
  (:require
   [datomic.api :as dtm]
   [io.rkn.conformity :as dtmcnf]
   [mur.components.datomic.conformer :as cptdtmcnf]
   [taoensso.encore :as u]))

(defn bootstrap-user-schema
  [conn]
  (let [db      (dtm/db conn)
        tx-data (u/conj-when
                 []
                 (when-not (dtmcnf/has-attribute? db :user/username)
                   {:db/id                 (dtm/tempid :db.part/user)
                    :db/ident              :user/username
                    :db/valueType          :db.type/string
                    :db/cardinality        :db.cardinality/one
                    :db/unique             :db.unique/value
                    :db/index              true
                    :db/isComponent        false
                    :db/noHistory          false
                    :db/fulltext           false
                    :db/doc                "Username for a user"
                    :db.install/_attribute :db.part/db})
                 (when-not (dtmcnf/has-attribute? db :user/password)
                   {:db/id                 (dtm/tempid :db.part/user)
                    :db/ident              :user/password
                    :db/valueType          :db.type/string
                    :db/cardinality        :db.cardinality/one
                    :db/index              true
                    :db/isComponent        false
                    :db/noHistory          false
                    :db/fulltext           false
                    :db/doc                "Password for a user"
                    :db.install/_attribute :db.part/db}))]
    [tx-data]))

(defn make-user-datomic-conformer
  []
  (cptdtmcnf/make-datomic-conformer "private/obs/user/norm_map.edn"))
