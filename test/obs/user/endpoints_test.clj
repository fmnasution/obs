(ns obs.user.endpoints-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [ring.util.http-status :as sts]
   [obs.user.endpoints :as usredp]
   [obs.test.mock :as tstmck]))

(defn- make-create-user-context
  [{:keys [create-user-validator body-params]
    :or   {create-user-validator true}}]
  {:config                {:auth-token-duration  1
                           :reset-token-duration 1}
   :datastore             (tstmck/make-mock-datastore [{:username "foobar"
                                                        :password "barfoo"}])
   :signer                (tstmck/make-mock-signer)
   :create-user-validator (tstmck/make-mock-validator create-user-validator)
   :body-params           body-params})

(defn- run-endpoint
  [endpoint context request]
  ((endpoint context) request))

(deftest create-user
  (testing "endpoint to register a user"
    (let [endpoint #(run-endpoint usredp/create-user-endpoint
                                  (make-create-user-context %1)
                                  %2)]
      (testing "only allow `:post` request method"
        (is (= sts/method-not-allowed
               (:status (endpoint {} {:request-method :get})))))
      (testing "malformed if no muuntaja format"
        (is (= sts/bad-request
               (:status (endpoint {} {:request-method :post})))))
      (testing "check if the content type is supported"
        (is (= sts/unsupported-media-type
               (:status (endpoint {}
                                  {:request-method  :post
                                   :muuntaja/format :foo
                                   :headers         {"content-type"
                                                     "application/foobar"}})))))
      (testing "attempt to validate body"
        (is (= sts/unprocessable-entity
               (:status (endpoint {:create-user-validator false}
                                  {:request-method  :post
                                   :muuntaja/format :foo
                                   :headers         {"content-type"
                                                     "application/json"}})))))
      (testing "check if target user is exists or no"
        (is (= sts/conflict
               (:status (endpoint
                         {:body-params {:username "foobar"
                                        :password "barfoo"}}
                         {:request-method  :post
                          :muuntaja/format :foo
                          :headers         {"content-type"
                                            "application/json"}})))))
      (testing "successfully create a user"
        (is (= sts/created
               (:status (endpoint
                         {:body-params {:username "barfoo"
                                        :password "foobar"}}
                         {:request-method  :post
                          :muuntaja/format :foo
                          :headers         {"content-type"
                                            "application/json"}}))))))))

(defn- make-reset-token-context
  [{:keys [forget-claims-validator route-params forget-claims]
    :or   {forget-claims-validator true}}]
  {:config                  {:auth-token-duration  1
                             :reset-token-duration 1}
   :datastore               (tstmck/make-mock-datastore [{:username "barfoo"
                                                          :password "barfoo"}])
   :signer                  (tstmck/make-mock-signer)
   :route-params            route-params
   :forget-claims           forget-claims
   :forget-claims-validator (tstmck/make-mock-validator
                             forget-claims-validator)})

(deftest reset-token
  (testing "endpoint to get reset token"
    (let [endpoint #(run-endpoint usredp/reset-token-endpoint
                                  (make-reset-token-context %1)
                                  %2)]
      (testing "only allow `:post` request method"
        (is (= sts/method-not-allowed
               (:status (endpoint {}
                                  {:request-method :delete})))))
      (testing "attempt to validate `:forget-claims`"
        (is (= sts/bad-request
               (:status (endpoint {:forget-claims-validator false}
                                  {:request-method :post})))))
      (testing "check on equality of target's username with the claims"
        (is (= sts/conflict
               (:status (endpoint {:route-params  {:username "barfoo"}
                                   :forget-claims {:username "foobar"}}
                                  {:request-method :post})))))
      (testing "check if target is exists or no"
        (is (= sts/not-found
               (:status (endpoint {:route-params  {:username "foobar"}
                                   :forget-claims {:username "foobar"}}
                                  {:request-method :post})))))
      (testing "successfully getting a reset token"
        (is (= sts/ok
               (:status (endpoint {:route-params  {:username "barfoo"}
                                   :forget-claims {:username "barfoo"}}
                                  {:request-method :post}))))))))

(defn- make-target-user-context
  [{:keys [body-params
           route-params
           user-credentials
           reset-claims
           user-credentials-validator
           reset-claims-validator
           update-password-validator]
    :or   {user-credentials-validator true
           reset-claims-validator     true
           update-password-validator  true}}]
  {:config                     {:auth-token-duration  1
                                :reset-token-duration 1}
   :datastore                  (tstmck/make-mock-datastore
                                [{:username "fooqux"
                                  :password "quxfoo"}])
   :signer                     (tstmck/make-mock-signer)
   :body-params                body-params
   :route-params               route-params
   :user-credentials           user-credentials
   :user-credentials-validator (tstmck/make-mock-validator
                                user-credentials-validator)
   :reset-claims               reset-claims
   :reset-claims-validator     (tstmck/make-mock-validator
                                reset-claims-validator)
   :update-password-validator  (tstmck/make-mock-validator
                                update-password-validator)})

(deftest target-user
  (testing "endpoint  to delete, update password and get auth token"
    (let [endpoint #(run-endpoint usredp/target-user-endpoint
                                  (make-target-user-context %1)
                                  %2)]
      (testing "only allow `:post`, `:put`, `:delete` request method"
        (is (= sts/method-not-allowed
               (:status (endpoint {} {:request-method :get})))))
      (testing "getting the auth token"
        (testing "check if user credentials format is valid"
          (is (= sts/bad-request
                 (:status (endpoint {:user-credentials-validator false}
                                    {:request-method :post})))))
        (testing "check if user credentials points to existing user"
          (is (= sts/unauthorized
                 (:status (endpoint {:user-credentials {:username "quxfoo"
                                                        :password "fooqux"}}
                                    {:request-method :post})))))
        (testing "check on equality of target's username with the credentials"
          (is (= sts/conflict
                 (:status (endpoint {:route-params     {:username "quxfoo"}
                                     :user-credentials {:username "fooqux"
                                                        :password "quxfoo"}}
                                    {:request-method :post})))))
        (testing "successfully getting an auth token"
          (is (= sts/ok
                 (:status (endpoint {:route-params     {:username "fooqux"}
                                     :user-credentials {:username "fooqux"
                                                        :password "quxfoo"}}
                                    {:request-method :post}))))))
      (testing "delete a user"
        (testing "check if user credentials format is valid"
          (is (= sts/bad-request
                 (:status (endpoint {:user-credentials-validator false}
                                    {:request-method :delete})))))
        (testing "check if user credentials points to existing user"
          (is (= sts/unauthorized
                 (:status (endpoint {:user-credentials {:username "quxfoo"
                                                        :password "fooqux"}}
                                    {:request-method :delete})))))
        (testing "check on equality of target's username with the credentials"
          (is (= sts/conflict
                 (:status (endpoint {:route-params     {:username "quxfoo"}
                                     :user-credentials {:username "fooqux"
                                                        :password "quxfoo"}}
                                    {:request-method :delete})))))
        (testing "successfully delete a user"
          (is (= sts/no-content
                 (:status (endpoint {:route-params     {:username "fooqux"}
                                     :user-credentials {:username "fooqux"
                                                        :password "quxfoo"}}
                                    {:request-method :delete}))))))
      (testing "update user's password"
        (testing "with credentials"
          (testing "malformed if no muuntaja format"
            (is (= sts/bad-request
                   (:status (endpoint {} {:request-method :put})))))
          (testing "check if user credentials format is valid"
            (is (= sts/bad-request
                   (:status (endpoint {:user-credentials-validator false}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check if the body is valid"
            (is (= sts/bad-request
                   (:status (endpoint {:update-password-validator false}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check if user credentials points to existing user"
            (is (= sts/unauthorized
                   (:status (endpoint {:user-credentials {:username "quxfoo"
                                                          :password "fooqux"}}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check on equality of target's username with the credentials"
            (is (= sts/conflict
                   (:status (endpoint {:route-params     {:username "quxfoo"}
                                       :user-credentials {:username "fooqux"
                                                          :password "quxfoo"}}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check if the content type is supported"
            (is (= sts/unsupported-media-type
                   (:status (endpoint
                             {:route-params     {:username "fooqux"}
                              :user-credentials {:username "fooqux"
                                                 :password "quxfoo"}}
                             {:request-method  :put
                              :muuntaja/format :foo
                              :headers         {"content-type"
                                                "application/foobar"}})))))
          (testing "successfully update the password and getting the auth token"
            (is (= sts/ok
                   (:status (endpoint
                             {:route-params     {:username "fooqux"}
                              :user-credentials {:username "fooqux"
                                                 :password "quxfoo"}
                              :body-params      {:password "foobarqux"}}
                             {:request-method  :put
                              :muuntaja/format :foo
                              :headers         {"content-type"
                                                "application/json"}}))))))
        (testing "with reset claims"
          (testing "malformed if no muuntaja format"
            (is (= sts/bad-request
                   (:status (endpoint {}
                                      {:request-method :put})))))
          (testing "check if the body is valid"
            (is (= sts/bad-request
                   (:status (endpoint {:update-password-validator false}
                                      {:request-method   :put
                                       :muuuntaja/format :foo})))))
          (testing "check if reset claims format is valid"
            (is (= sts/bad-request
                   (:status (endpoint {:reset-claims-validator false
                                       :reset-claims           {}}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check on equality of target's username with the claims"
            (is (= sts/conflict
                   (:status (endpoint {:route-params {:username "fooqux"}
                                       :reset-claims {:username "quxfoo"}}
                                      {:request-method  :put
                                       :muuntaja/format :foo})))))
          (testing "check if the content type is supported"
            (is (= sts/unsupported-media-type
                   (:status (endpoint
                             {:route-params {:username "fooqux"}
                              :reset-claims {:username "fooqux"}}
                             {:request-method  :put
                              :muuntaja/format :foo
                              :headers         {"content-type"
                                                "application/yaml"}})))))
          (testing "successfully update the password and getting the auth token"
            (is (= sts/ok
                   (:status (endpoint
                             {:route-params {:username "fooqux"}
                              :reset-claims {:username "fooqux"
                                             :password "quxfoo"}
                              :body-params  {:password "foobarqux"}}
                             {:request-method  :put
                              :muuntaja/format :foo
                              :headers         {"content-type"
                                                "application/json"}}))))))))))
