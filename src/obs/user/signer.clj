(ns obs.user.signer
  (:require
   [clojure.spec.alpha :as s]
   [clj-time.core :as t]
   [com.stuartsierra.component :as c]
   [buddy.core.keys :as bdyks]
   [buddy.sign.jwt :as bdysgnjwt]))

;; =================================================================
;; sha signer spec
;; =================================================================

(s/def ::size
  pos-int?)

(s/def ::secret
  (s/nilable string?))

(s/def ::sha-signer-option
  (s/keys :req-un [::size]
          :opt-un [::secret]))

;; =================================================================
;; asymetric signer spec
;; =================================================================

(s/def ::algorithm
  #{:es256 :es512 :ps256 :ps512 :rs256 :rs512})

(s/def ::public-key-path
  string?)

(s/def ::private-key-path
  string?)

(s/def ::asymetric-signer-option
  (s/keys :req-un [::algorithm ::public-keypath ::private-key-path]))

;; =================================================================
;; signer spec
;; =================================================================

(s/def ::kind
  #{:sha :asymetric})

(s/def ::signer-option
  (s/keys :req-un [::kind]))

;; =================================================================
;; protocols spec
;; =================================================================

(s/def ::sign-output
  string?)

(s/def ::unsign-output
  map?)

;; =================================================================
;; token spec
;; =================================================================

(s/def ::id
  (s/or :string string?
        :int    pos-int?))

(s/def ::username
  string?)

(s/def ::token-user
  (s/keys :req-un [::id ::username]))

;; =================================================================
;; protocols
;; =================================================================

(defprotocol ISigner
  (-sign [this data])
  (-unsign [this signed-data]))

;; =================================================================
;; sha signer
;; =================================================================

(defn- sha-kind
  [size]
  (if (= 256 size)
    :hs256
    :hs512))

(defrecord SHASigner [size secret]
  ISigner
  (-sign [this data]
    (let [alg (sha-kind size)]
      (bdysgnjwt/sign data secret {:alg alg})))
  (-unsign [this signed-data]
    (let [alg (sha-kind size)]
      (bdysgnjwt/unsign signed-data secret {:alg alg}))))

(defn make-sha-signer
  [option]
  (-> (s/assert ::sha-signer-option option)
      (select-keys [:size :secret])
      (map->SHASigner)))

;; =================================================================
;; asymetric signer
;; =================================================================

(defrecord AsymetricSigner [public-key-path
                            private-key-path
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
  (-sign [this data]
    (bdysgnjwt/sign data private-key {:alg algorithm}))
  (-unsign [this signed-data]
    (bdysgnjwt/unsign signed-data public-key {:alg algorithm})))

(defn make-asymetric-signer
  [option]
  (-> (s/assert ::asymetric-signer-option option)
      (select-keys [:algorithm
                    :private-key-path
                    :public-key-path])
      (map->AsymetricSigner)))

(defn make-signer
  [config]
  (case (:kind (s/assert ::signer-option config))
    :sha       (make-sha-signer config)
    :asymetric (make-asymetric-signer config)))

(defn sign
  [signer data]
  (->> data
       (-sign signer)
       (s/assert ::sign-output)))

(defn unsign
  [signer signed-data]
  (->> signed-data
       (-unsign signer)
       (s/assert ::unsign-output)))

(defn auth-token
  ([signer user hours-expired]
   (let [now (t/now)
         exp (t/plus now (t/hours (if (some? hours-expired)
                                    hours-expired
                                    1)))]
     (-> (s/assert ::token-user user)
         (select-keys [:id :username])
         (assoc :iat now :exp exp)
         (as-> <> (sign signer <>)))))
  ([signer user]
   (auth-token signer user nil)))

(defn reset-token
  ([signer user hours-expired]
   (let [now (t/now)
         exp (t/plus now (t/hours (if (some? hours-expired)
                                    hours-expired
                                    1)))]
     (-> (s/assert ::token-user user)
         (select-keys [:id :username])
         (assoc :sub "reset", :iat now, :exp exp)
         (as-> <> (sign signer <>)))))
  ([signer user]
   (reset-token signer user nil)))
