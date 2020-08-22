(ns fr.jeremyschoffen.mapiform.core-test
  (:require
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])
    [fr.jeremyschoffen.mapiform.core :as c]))


(deftest assoc-tests
  (is (= (c/assoc-computed {}
           :a (constantly 1)
           :b #(-> % :a inc)
           :c (fn [{:keys [a b]}]
                  (+ a b)))
         {:a 1
          :b 2
          :c 3}))

  (is (= (c/ensure-computed {:b 5}
           :a (constantly 1)
           :b #(-> % :a inc)
           :c (fn [{:keys [a b]}]
                (+ a b)))
         {:a 1
          :b 5
          :c 6}))

  (is (= (c/augment-computed {:a {:aa 1}}
           :a (constantly {:ab 3})
           :b #(-> % :a))
         {:a {:aa 1, :ab 3}
          :b {:aa 1, :ab 3}})))


