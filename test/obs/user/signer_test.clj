(ns obs.user.signer-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is are]]
   [com.stuartsierra.component :as c]
   [obs.user.signer :as usrsgn]))

(defn- sha-signer-system
  [m]
  (c/system-map
   :signer (usrsgn/make-sha-signer m)))

(deftest sha-signer
  (testing "able to sign and unsign claims"
    (let [system (sha-signer-system
                  {:size      512
                   :secret    "my secret that no one knows"
                   :auth-exp  1
                   :reset-exp 1})
          {:keys [signer] :as started}
          (c/start system)]
      (testing "signing claims"
        (let [signed (usrsgn/sign signer {:foo "bar"})]
          (is (string? signed))
          (testing "unsigning claims"
            (is (map? (usrsgn/unsign signer signed))))))
      (testing "signing auth token"
        (let [signed (usrsgn/auth-token signer {:id 1 :username "foobar"})]
          (is (string? signed))
          (testing "unsigning auth token"
            (let [unsigned (usrsgn/unsign signer signed)]
              (is (map? unsigned))
              (are [k] (contains? unsigned k)
                :id
                :username
                :iat
                :exp)))))
      (testing "signing reset token"
        (let [signed (usrsgn/reset-token signer {:id 2 :username "barfoo"})]
          (is (string? signed))
          (testing "unsigning reset token"
            (let [unsigned (usrsgn/unsign signer signed)]
              (is (map? unsigned))
              (are [k] (contains? unsigned k)
                :id
                :username
                :iat
                :exp
                :sub)
              (is (= "reset" (:sub unsigned)))))))
      (c/stop started))))

(defn- asymetric-signer-system
  [m]
  (c/system-map
   :signer (usrsgn/make-asymetric-signer m)))

(deftest asymetryc-signer
  (testing "able to sign and unsign claims"
    (let [system (asymetric-signer-system
                  {:algorithm        :rs256
                   :private-key-path (io/resource
                                      "private/obs/key/obstestkey.pem")
                   :public-key-path  (io/resource
                                      "private/obs/key/obstestkey_public.pem")
                   :auth-exp         1
                   :reset-exp        1})
          {:keys [signer] :as started}
          (c/start system)]
      (testing "signing claims"
        (let [signed (usrsgn/sign signer {:foo "bar"})]
          (is (string? signed))
          (testing "unsigning claims"
            (is (map? (usrsgn/unsign signer signed))))))
      (testing "signing auth token"
        (let [signed (usrsgn/auth-token signer {:id 1 :username "foobar"})]
          (is (string? signed))
          (testing "unsigning auth token"
            (let [unsigned (usrsgn/unsign signer signed)]
              (is (map? unsigned))
              (are [k] (contains? unsigned k)
                :id
                :username
                :iat
                :exp)))))
      (testing "signing reset token"
        (let [signed (usrsgn/reset-token signer {:id 2 :username "barfoo"})]
          (is (string? signed))
          (testing "unsigning reset token"
            (let [unsigned (usrsgn/unsign signer signed)]
              (is (map? unsigned))
              (are [k] (contains? unsigned k)
                :id
                :username
                :iat
                :exp
                :sub)
              (is (= "reset" (:sub unsigned)))))))
      (c/stop started))))
