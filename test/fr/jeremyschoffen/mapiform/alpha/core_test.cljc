(ns fr.jeremyschoffen.mapiform.alpha.core-test
  (:require
    #?(:clj [clojure.test :refer :all]
       :cljs [cljs.test :refer-macros [deftest is testing]])
    [fr.jeremyschoffen.mapiform.alpha.core :as c]))


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



(def ^:dynamic *recorder* nil)


(defn record! [x]
  (when *recorder*
    (swap! *recorder* conj x)))


(defn wrap-recorder [f]
  (fn [arg]
    (record! arg)
    (f arg)))


(deftest side-effects
  (is (= (-> 1
             inc
             (c/thread-fns dec
                           (c/side-effect! println)
                           inc)
             (c/side-effect! println)
             dec)
         1))

  (is (= (binding [*recorder* (atom [])]
           (-> 1
               inc
               (c/thread-fns dec
                             (c/wrapped-side-effect! println wrap-recorder)
                             inc)
               (c/wrapped-side-effect! println wrap-recorder)
               dec
               vector
               (conj @*recorder*)))
         [1 [1 2]])))
