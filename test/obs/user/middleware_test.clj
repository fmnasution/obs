(ns obs.user.middleware-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [buddy.core.codecs :as bdycdc]
   [buddy.core.codecs.base64 :as bdycdc64]
   [obs.user.signer :as usrsgn]
   [obs.user.middleware :as usrmdw]))

(deftest extracting-credentials
  (testing "get credentials from authorization headers"
    (let [handler (usrmdw/-wrap-user-credentials identity)
          token   (bdycdc/bytes->str (bdycdc64/encode "foobar:barfoo"))]
      (testing "has the correct header value"
        (is (map? (:user-credentials
                   (handler
                    {:headers {"authorization" (str "Basic " token)}
                     :uri     "/"})))))
      (testing "doesn't contain the header"
        (is (nil? (:user-credentials (handler {:uri "/"})))))
      (testing "wrong scheme"
        (is (nil? (:user-credentials
                   (handler {:uri     "/"
                             :headers {"authorization" "Foobar 123ff"}})))))
      (testing "malformed token"
        (is (= {}
               (:user-credentials
                (handler {:uri     "/"
                          :headers {"authorization" "Basic acwegwefew"}}))))))))

(deftest extracting-forget-claims
  (testing "get forget claims from authorization headers"
    (let [signer  (usrsgn/make-sha-signer
                   {:size      512
                    :secret    "abcdefghijklmnopqrstuvwxyz"
                    :auth-exp  1
                    :reset-exp 1})
          token   (usrsgn/sign signer {:foo :bar})
          handler (usrmdw/-wrap-forget-password-claims identity signer)]
      (testing "has the correct header value"
        (is (map? (:forget-claims
                   (handler {:uri     "/"
                             :headers {"authorization"
                                       (str "ObsForget " token)}})))))
      (testing "wrong scheme"
        (is (nil? (:forget-claims
                   (handler {:uri     "/"
                             :headers {"authorization" "Foobar 123ff"}})))))
      (testing "doesn't contain the header"
        (is (nil? (:forget-claims (handler {:uri "/"}))))))))

(deftest extracting-reset-claims
  (testing "get reset claims from authorization headers"
    (let [signer  (usrsgn/make-sha-signer
                   {:size      512
                    :secret    "abcdefghijklmnopqrstuvwxyz"
                    :auth-exp  1
                    :reset-exp 1})
          token (usrsgn/sign signer {:foo :bar})
          handler (usrmdw/-wrap-reset-password-claims identity nil)]
      (testing "has the correct header value"
        (is (map? (:reset-claims
                   (handler {:uri     "/"
                             :headers {"authorization"
                                       (str "ObsReset " token)}})))))
      (testing "wrong scheme"
        (is (nil? (:reset-claims
                   (handler {:uri     "/"
                             :headers {"authorization" "Foobar 123ff"}})))))
      (testing "doesn't contain the header"
        (is (nil? (:reset-claims (handler {:uri "/"}))))))))
