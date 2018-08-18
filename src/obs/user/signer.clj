(ns obs.user.signer
  (:require
   [clj-time.core :as t]
   [com.stuartsierra.component :as c]
   [buddy.core.keys :as bdyks]
   [buddy.sign.jwt :as bdysgnjwt]))

;; =================================================================
;; protocols
;; =================================================================

(defprotocol ISigner
  (sign [this data])
  (unsign [this signed-data]))

;; =================================================================
;; sha signer
;; =================================================================

(defn- sha-kind
  [size]
  (if (= 256 size)
    :hs256
    :hs512))

(defrecord SHASigner [size secret auth-exp reset-exp]
  ISigner
  (sign [this data]
    (let [alg (sha-kind size)]
      (bdysgnjwt/sign data secret {:alg alg})))
  (unsign [this signed-data]
    (let [alg (sha-kind size)]
      (bdysgnjwt/unsign signed-data secret {:alg alg}))))

(defn make-sha-signer
  [option]
  (-> option
      (select-keys [:size :secret :auth-exp :reset-exp])
      (map->SHASigner)))

;; =================================================================
;; asymetric signer
;; =================================================================

(defrecord AsymetricSigner [public-key-path
                            private-key-path
                            auth-exp
                            reset-exp
                            algorithm
                            public-key
                            private-key]
  c/Lifecycle
  (start [this]
    (if (some? private-key)
      this
      (let [pubkey  (bdyks/public-key public-key-path)
            privkey (bdyks/private-key private-key-path)]
        (assoc this :public-key pubkey :private-key privkey))))
  (stop [this]
    (if (nil? private-key)
      this
      (assoc this :public-key nil :private-key nil)))

  ISigner
  (sign [this data]
    (bdysgnjwt/sign data private-key {:alg algorithm}))
  (unsign [this signed-data]
    (bdysgnjwt/unsign signed-data public-key {:alg algorithm})))

(defn make-asymetric-signer
  [option]
  (-> option
      (select-keys [:algorithm
                    :private-key-path
                    :public-key-path
                    :auth-exp
                    :reset-exp])
      (map->AsymetricSigner)))

(defn make-signer
  [config]
  (case (:kind config)
    :sha       (make-sha-signer config)
    :asymetric (make-asymetric-signer config)))

(defn auth-token
  [signer user]
  (let [now (t/now)
        exp (t/plus now (t/hours (:auth-exp signer)))]
    (-> user
        (select-keys [:id :username])
        (assoc :iat now :exp exp)
        (as-> <> (sign signer <>)))))

(defn reset-token
  [signer user]
  (let [now (t/now)
        exp (t/plus now (t/hours (:reset-exp signer)))]
    (-> user
        (select-keys [:id :username])
        (assoc :sub "reset", :iat now, :exp exp)
        (as-> <> (sign signer <>)))))
