(ns obs.user.api-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [com.stuartsierra.component :as c]
   [mur.components.datomic.conformer :as cptdtmcnf]
   [mur.components.datomic :as cptdtm]
   [obs.user.api :as usrapi]))

;; (defn- datomic-system
;;   [m]
;;   (-> (c/system-map
;;        :datomic-conformer (cptdtmcnf/make-datomic-conformer
;;                            "private/obs/norm_map.edn")
;;        :datomic-blueprint (cptdtm/make-temp-datomic-db
;;                            (:datomic-blueprint m))
;;        :datomic           (cptdtm/make-datomic-conn))
;;       (c/system-using
;;        {:datomic           {:datomic-db :datomic-blueprint}
;;         :datomic-conformer {:datomic-conn :datomic}})))

;; (let [system (datomic-system
;;               {:datomic-blueprint {:uri "datomic:mem://obs-test"}})]
;;   (deftest datomic-user-api
;;     (testing "create and return a user"
;;       (let [started (c/start system)]
;;         (is (= 1 2))))))
