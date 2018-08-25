(ns obs.endpoints.adapter-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [obs.endpoints.adapter :as edpapt]))

(deftest applying-context
  (testing "flip the order of context and request"
    (let [handler (fn [context another-context]
                    (fn [request]
                      (assoc request
                             :context         context
                             :another-context another-context)))]
      (is (= {:request-method  :get
              :uri             "/"
              :context         {:a :b}
              :another-context {:c :d}}
             (((edpapt/context-adapter handler)
               {:request-method :get
                :uri            "/"})
              {:a :b}
              {:c :d}))))))
