(ns obs.endpoints.decisions-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [obs.endpoints.decisions :as edpdcs]
   [obs.test.mock :as tstmck]))

(deftest specific-request-method
  (testing "run the given `f` if only `:request-method` is in `request-methods`"
    (let [handler (edpdcs/run-on-request-method (constantly {:foo :bar})
                                                [:get :put]
                                                :default)]
      (testing "handler is invoked"
        (is (handler {:request {:request-method :get}})))
      (testing "default value is returned"
        (is (= :default (handler {:request {:request-method :post}})))))))

(deftest authenticate-with-credentials
  (testing "return user with the given `user-credentials`"
    (let [datastore (tstmck/make-mock-datastore [{:username "foo"
                                                  :password "bar"}])]
      (testing "credentials matched"
        (is (some? (:self (edpdcs/check-authenticated datastore
                                                      {:username "foo"
                                                       :password "bar"})))))
      (testing "no credentials matched"
        (is (nil? (edpdcs/check-authenticated datastore
                                              {:username "bar"
                                               :password "foo"})))))))

(deftest check-content-type
  (testing "check if content type is exists based on `muuntaja`"
    (testing "contains parsable content type"
      (is (false? (edpdcs/no-content-type? {:request {:muuntaja/format :foo}}))))
    (testing "contains no parsable content type"
      (is (true? (edpdcs/no-content-type? {:request {}})))))
  (testing "check if content type is supported"
    (let [f #(edpdcs/supported-content-type? % ["application/json"])]
      (testing "supported"
        (is (true? (f {:request {:headers {"content-type" "application/json"}}}))))
      (testing "not supported"
        (is (false? (f {:request {:headers {"content-type" "application/yaml"}}}))))
      (testing "false if no content type at all"
        (is (false? (f {:request {}})))))))
