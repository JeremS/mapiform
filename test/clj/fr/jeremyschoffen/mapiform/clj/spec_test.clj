(ns fr.jeremyschoffen.mapiform.clj.spec-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [orchestra.spec.test :as st]
    [fr.jeremyschoffen.mapiform.clj.spec :as ms]))


(s/def ::a int?)
(s/def ::x int?)
(s/def ::y int?)


(defn foo [{a ::a}]
  (inc a))

(ms/spec-op foo
            :param {:req [::a]})


(defn bar [{x ::x
            y ::y}]
  (- x y))

(ms/spec-op bar
            :param {:req [::x ::y]}
            :fn #(> (-> % :args :param ::x)
                    (-> % :args :param ::y)))


(defn baz [_])

(ms/spec-op baz
            :deps [foo bar])


(st/instrument `bar)


(deftest basics
  (testing "instrumentation works"
    (is (thrown? Exception
                 (bar {})))

    (is (thrown? Exception
                 (bar {::x 1 ::y 3})))))