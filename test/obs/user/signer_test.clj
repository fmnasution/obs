(ns obs.user.signer-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest testing is are]]
   [com.stuartsierra.component :as c]
   [obs.user.signer :as usrsgn]))

(defn- sha-signer-system
  [m]
  (c/system-map
   :signer (usrsgn/make-sha-signer (:signer m))))

(let [system (sha-signer-system
              {:signer {:size      512
                        :secret    "abcdefghijklmnopqrstuvwxyz"
                        :auth-exp  1
                        :reset-exp 1}})]
  (deftest sha-signer
    (testing "able to sign and unsign a claims"
      (let [started  (c/start system)
            signed   (usrsgn/sign (:signer started)
                                  {:foo     "bar"
                                   :baz     {:bar :quux
                                             :hmm [1 2 3]
                                             :hii #{:a :b :c}}
                                   "foobar" "barquux"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (not= {:foo     "bar"
                   :baz     {:bar :quux
                             :hmm [1 2 3]
                             :hii #{:a :b :c}}
                   "foobar" "barquux"}
                  signed))
        (is (= {:foo    "bar"
                :baz    {:bar "quux"
                         :hmm [1 2 3]
                         :hii ["a" "b" "c"]}
                :foobar "barquux"}
               (update-in unsigned [:baz :hii] sort)))
        (c/stop started)))
    (testing "able to create auth token"
      (let [started  (c/start system)
            signed   (usrsgn/auth-token (:signer started)
                                        {:id       1
                                         :username "foobar"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (string? signed))
        (are [k] (contains? unsigned k)
          :id
          :username
          :iat
          :exp)
        (c/stop started)))
    (testing "able to create reset token"
      (let [started (c/start system)
            signed   (usrsgn/reset-token (:signer started)
                                         {:id       1
                                          :username "foobar"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (string? signed))
        (are [k] (contains? unsigned k)
          :id
          :username
          :iat
          :exp
          :sub)
        (is (= "reset" (:sub unsigned)))
        (c/stop started)))))

(defn- asymetric-signer-system
  [m]
  (c/system-map
   :signer (usrsgn/make-asymetric-signer (:signer m))))

(let [system (asymetric-signer-system
              {:signer
               {:algorithm        :rs256
                :private-key-path (io/resource
                                   "private/obs/key/obstestkey.pem")
                :public-key-path  (io/resource
                                   "private/obs/key/obstestkey_public.pem")
                :auth-exp         1
                :reset-exp        1}})]
  (deftest asymetric-signer
    (testing "able to sign and unsign a claims"
      (let [started  (c/start system)
            signed   (usrsgn/sign (:signer started)
                                  {:foo     "bar"
                                   :baz     {:bar :quux
                                             :hmm [1 2 3]
                                             :hii #{:a :b :c}}
                                   "foobar" "barquux"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (not= {:foo     "bar"
                   :baz     {:bar :quux
                             :hmm [1 2 3]
                             :hii #{:a :b :c}}
                   "foobar" "barquux"}
                  signed))
        (is (= {:foo    "bar"
                :baz    {:bar "quux"
                         :hmm [1 2 3]
                         :hii ["a" "b" "c"]}
                :foobar "barquux"}
               (update-in unsigned [:baz :hii] sort)))
        (c/stop started)))
    (testing "able to create auth token"
      (let [started  (c/start system)
            signed   (usrsgn/auth-token (:signer started)
                                        {:id       2
                                         :username "barfoo"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (string? signed))
        (are [k] (contains? unsigned k)
          :id
          :username
          :iat
          :exp)
        (c/stop started)))
    (testing "able to create reset token"
      (let [started  (c/start system)
            signed   (usrsgn/reset-token (:signer started)
                                         {:id       2
                                          :username "barfoo"})
            unsigned (usrsgn/unsign (:signer started) signed)]
        (is (string? signed))
        (are [k] (contains? unsigned k)
          :id
          :username
          :iat
          :exp
          :sub)
        (is (= "reset" (:sub unsigned)))
        (c/stop started)))))
