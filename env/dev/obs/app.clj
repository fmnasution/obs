(ns obs.app
  (:require
   [clojure.java.io :as io]
   [com.stuartsierra.component :as c]
   [aero.core :refer [read-config]]
   [obs.main.bootstrap :as mnbtst]
   [obs.user.bootstrap :as usrbtst]
   [obs.system :as sys]))

;; ================================================================
;; app
;; ================================================================

(defn make-dev-system-map
  []
  (let [config (read-config
                (io/resource "private/obs/config.edn")
                {:profile :dev})]
    (-> (sys/make-system-map config)
        (assoc :main-datastore-bootstrapper
               (mnbtst/make-datastore-bootstrapper
                (:datastore config))

               :user-datastore-bootstrapper
               (usrbtst/make-datastore-bootstrapper
                (:datastore config)))
        (c/system-using
         {:main-datastore-bootstrapper {:datomic-conn :datastore}
          :user-datastore-bootstrapper {:datomic-conn :datastore}}))))
