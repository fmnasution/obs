(ns obs.main.middleware-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [ring.util.http-status :as sts]
   [obs.main.middleware :as mnmdw]))

(deftest trailing-slash
  (testing "ignore one trailing slash in `:uri`"
    (let [handler (mnmdw/-wrap-trailing-slash identity)]
      (testing "do nothing"
        (= "/foobar" (:uri (handler {:uri "/foobar"}))))
      (testing "ignored"
        (= "/foobar" (:uri (handler {:uri "/foobar/"}))))
      (testing "actually only 1 is being ignored"
        (= "/foobar///" (:uri (handler {:uri "/foobar////"})))))))

(deftest catching-exception
  (testing "catching exception in handler"
    (testing "handler is throwing an exception"
      (is (= 500 (:status ((mnmdw/-wrap-exception #(/ % 0))
                           {:status 200})))))
    (testing "handler isn't throwing an exception"
      (is (= 200 (:status ((mnmdw/-wrap-exception identity)
                           {:status 200})))))))
