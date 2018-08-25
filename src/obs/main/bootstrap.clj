(ns obs.main.bootstrap
  (:require
   [datomic.api :as dtm]
   [io.rkn.conformity :as dtmcnf]
   [mur.components.datomic.conformer :as cptdtmcnf]
   [taoensso.encore :as u]))

(defn bootstrap-main-schema
  [conn]
  (let [db      (dtm/db conn)
        tx-data (u/conj-when
                 []
                 (when-not (dtmcnf/has-attribute? db :db.entity/id)
                   {:db/id                 (dtm/tempid :db.part/user)
                    :db/ident              :db.entity/id
                    :db/valueType          :db.type/string
                    :db/cardinality        :db.cardinality/one
                    :db/unique             :db.unique/identity
                    :db/index              true
                    :db/isComponent        false
                    :db/noHistory          false
                    :db/fulltext           false
                    :db/doc                "Unique id for everyone"
                    :db.install/_attribute :db.part/db}))]
    [tx-data]))

(defn make-main-datomic-conformer
  []
  (cptdtmcnf/make-datomic-conformer "private/obs/main/norm_map.edn"))
