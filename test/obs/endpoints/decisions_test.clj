(ns obs.endpoints.decisions-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [obs.user.api :as usrapi]
   [obs.endpoints.decisions :as edpdcs]))

(deftest specific-request-method
  (testing "run the given `f` if only `:request-method` is in `request-methods`"
    (is (= {:foo :bar}
           ((edpdcs/run-on-request-method (constantly {:foo :bar})
                                          [:get]
                                          :default)
            {:request {:request-method :get}})))
    (is (= :default
           ((edpdcs/run-on-request-method (constantly {:bar :foo})
                                          [:post :put]
                                          :default)
            {:request {:request-method :delete}})))))

(defn- user-datastore
  [user-map]
  (reify usrapi/IUserAPI
    (get-by-username [_ username]
      (get user-map username))
    (update-password-by-username [_ username password]
      (when (contains? user-map username)
        (user-datastore (assoc-in user-map [username :password] password))))
    (delete-by-username [_ username]
      (user-datastore (dissoc user-map username)))
    (create-and-return [_ {:keys [username] :as user}]
      (let [new-user-map (assoc user user username)]
        [new-user-map (user-datastore new-user-map)]))
    (authenticate [_ {:keys [username password]}]
      (when-let [user (get user-map username)]
        (when (= (:password user) password)
          user)))))

(deftest authenticate-with-credentials
  (testing "return user with the given `user-credentials`"
    (let [datastore (user-datastore {"foo" {:username "foo"
                                            :password "bar"}
                                     "bar" {:username "bar"
                                            :password "foo"}})]
      (are [pred credentials] (pred (edpdcs/check-authenticated datastore
                                                                credentials))
        (comp = {:self {:username "foo"
                     :password "bar"}}) {:username "foo"
                                         :password "bar"}
        (comp = {:self {:username "bar"
                     :password "foo"}}) {:username "bar"
                                         :password "foo"}
        nil?                            {:username "foo"
                                         :password "foo"}
        nil?                            {:username "bar"
                                         :password "bar"}))))

(deftest check-content-type
  (testing "check if content type is exists based on `muuntaja`"
    (is (false? (edpdcs/no-content-type? {:request {:muuntaja/format :foo}})))
    (is (true? (edpdcs/no-content-type? {:request {}}))))
  (testing "check if content type is supported"
    (is (true? (edpdcs/supported-content-type?
                {:request {:headers {"content-type" "application/json"}}}
                ["application/json"])))
    (is (false? (edpdcs/supported-content-type?
                 {:request {:headers {"content-type" "application/yaml"}}}
                 ["application/json"])))
    (is (false? (edpdcs/supported-content-type?
                 {:request {:request-method :get}}
                 ["application/json"])))))
