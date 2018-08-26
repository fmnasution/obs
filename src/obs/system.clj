(ns obs.system
  (:require
   [com.stuartsierra.component :as c]
   [mur.components.http-kit :as cpthkit]
   [mur.components.ring :as cptrng]
   [io.clojure.liberator-transit]
   [obs.main.router :as mnrtr]
   [obs.main.middleware :as mnmdw]
   [obs.main.datastore.datomic :as mndtstdtm]
   [obs.main.datastore :as mndtst]
   [obs.main.logger :as mnlgr]
   [obs.main.bootstrap :as mnbtst]
   [obs.user.middleware :as usrmdw]
   [obs.user.signer :as usrsgn]
   [obs.user.endpoints :as usredp]
   [obs.user.validator :as usrvldt]
   [obs.user.bootstrap :as usrbtst]
   [obs.endpoints.middleware :as edpmdw]))

;; ================================================================
;; system
;; ================================================================

(defn make-system-map
  [config]
  (-> (c/system-map
       :web-server (cpthkit/make-web-server (:web-server config))
       :ring-head (cptrng/make-web-request-handler-head)
       :ring-router (mnrtr/make-ring-router)
       :ring-middleware (mnmdw/make-ring-middleware (:app config))
       :user-middleware (usrmdw/make-ring-middleware)
       :endpoint-middleware (edpmdw/make-ring-middleware (:app config))
       :datomic-blueprint (mndtstdtm/make-datomic-blueprint
                           (:datomic-blueprint config))
       :datastore (mndtst/make-datastore (:datastore config))
       :main-datastore-bootstrapper (mnbtst/make-datastore-bootstrapper
                                     (:datastore config))
       :user-datastore-bootstrapper (usrbtst/make-datastore-bootstrapper
                                     (:datastore config))
       :signer (usrsgn/make-signer (:signer config))
       :logger (mnlgr/make-logger (:logger config))
       :create-user-endpoint (usredp/make-create-user-endpoint)
       :reset-token-endpoint (usredp/make-reset-token-endpoint)
       :target-user-endpoint (usredp/make-target-user-endpoint)
       :create-user-validator (usrvldt/make-create-user-validator)
       :user-credentials-validator (usrvldt/make-user-credentials-validator)
       :forget-claims-validator (usrvldt/make-forget-claims-validator)
       :reset-claims-validator (usrvldt/make-reset-claims-validator)
       :update-password-validator (usrvldt/make-update-password-validator))
      (c/system-using
       {:web-server                  {:handler :ring-head}
        :ring-head                   {:handler    :ring-router
                                      :middleware :ring-middleware}
        :ring-middleware             {:middleware :user-middleware}
        :user-middleware             {:middleware :endpoint-middleware}
        :datastore                   {:datomic-db :datomic-blueprint}
        :main-datastore-bootstrapper {:datomic-conn :datastore}
        :user-datastore-bootstrapper {:datomic-conn :datastore}})
      (c/system-using
       {:ring-router         [:create-user-endpoint
                              :reset-token-endpoint
                              :target-user-endpoint]
        :ring-middleware     [:logger]
        :user-middleware     [:signer]
        :endpoint-middleware [:datastore
                              :signer
                              :logger
                              :create-user-validator
                              :user-credentials-validator
                              :forget-claims-validator
                              :reset-claims-validator
                              :update-password-validator]})))
