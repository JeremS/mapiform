(ns fr.jeremyschoffen.mapiform.cljs.spec-test
  (:require
    [cljs.test :refer [deftest is testing]]
    [cljs.spec.alpha :as s :include-macros true]
    [orchestra-cljs.spec.test :as st :include-macros true]
    [fr.jeremyschoffen.mapiform.cljs.spec :as ms :include-macros true]))


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
    (is (thrown? :default
          (bar {})))

    (is (thrown? :default
          (bar {::x 1 ::y 3})))))




(cljs.test/run-tests)


(comment

  (st/unstrument `bar)
  (clojure.repl/doc bar)

  (bar {::x 2 ::y 2}))


