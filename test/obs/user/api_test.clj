(ns obs.user.api-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [obs.user.api :as usrapi]
   [obs.test.mock :as tstmck]))

(deftest authenticate-user
  (testing "authenticate user with the given credentials"
    (let [datastore (tstmck/make-mock-datastore [{:username "foobar"
                                                  :password "barfoo"}])]
      (testing "matching credentials"
        (is (map? (usrapi/authenticate datastore {:username "foobar"
                                                  :password "barfoo"}))))
      (testing "wrong password"
        (is (nil? (usrapi/authenticate datastore {:username "foobar"
                                                  :password "foobar"}))))
      (testing "target non-existing user"
        (is (nil? (usrapi/authenticate datastore {:username "barfoo"
                                                  :password "foobar"})))))))
