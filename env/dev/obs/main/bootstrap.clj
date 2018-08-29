(ns obs.main.bootstrap
  (:require
   [clojure.spec.alpha :as s]
   [datomic.api :as dtm]
   [io.rkn.conformity :as dtmcnf]
   [mur.components.datomic.conformer :as cptdtmcnf]
   [taoensso.encore :as u]
   [obs.main.datastore :as mndtst]))

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

(defn make-datomic-conformer
  []
  (cptdtmcnf/make-datomic-conformer "private/obs/main/norm_map.edn"))

(defn make-datastore-bootstrapper
  [config]
  (case (:kind (s/assert ::mndtst/config config))
    :datomic (make-datomic-conformer)))
