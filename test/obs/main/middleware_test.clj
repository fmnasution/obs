(ns obs.main.middleware-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [ring.util.http-status :as sts]
   [obs.main.middleware :as mnmdw]))

(deftest trailing-slash
  (testing "ignore trailing slash in `:uri`"
    (are [response request] (= response
                               ((mnmdw/-wrap-trailing-slash identity) request))
      {:uri "/foobar"} {:uri "/foobar"}
      {:uri "/barfoo"} {:uri "/barfoo/"}
      {:uri "/bazbar/"} {:uri "/bazbar//"})))

(deftest catching-exception
  (testing "catching exception in handler"
    (is (= 500 (:status ((mnmdw/-wrap-exception #(/ % 0))
                         {:status 200}))))
    (is (= 200 (:status ((mnmdw/-wrap-exception identity)
                         {:status 200}))))))
