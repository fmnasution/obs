(ns obs.endpoints.middleware-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [obs.endpoints.middleware :as edpmdw]))

(deftest context-injection
  (testing "inject context for given handler"
    (let [handler (edpmdw/-inject-context (fn [request]
                                            (fn [context]
                                              {:request request
                                               :context context}))
                                          {:a :b}
                                          [:foobar :barfoo])]
      (is (= {:request {:request-method :get
                        :uri            "/"
                        :foobar         :bazbar
                        :barfoo         :bazquux}
              :context {:a      :b
                        :foobar :bazbar
                        :barfoo :bazquux}}
             (handler {:request-method :get
                       :uri            "/"
                       :foobar         :bazbar
                       :barfoo         :bazquux}))))))
