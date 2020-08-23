(ns fr.jeremyschoffen.mapiform.spec-test
  #?(:clj (:require
            [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [fr.jeremyschoffen.mapiform.clj.spec :as ms])

     :cljs (:require
             [cljs.test :refer [deftest is testing are]]
             [cljs.spec.alpha :as s :include-macros true]
             [orchestra-cljs.spec.test :as st :include-macros true]
             [fr.jeremyschoffen.mapiform.cljs.spec :as ms :include-macros true])))


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


(defn baz [{z ::z
            :or {z "prefix"}
            :as param}]
  (let [a (foo param)
        b (bar param)]
    (str z " " (+ a b))))


(ms/spec-op baz
            :deps [foo bar]
            :param {:opt [::a ::z]})


(st/instrument `[foo bar baz])


(deftest basics
  (testing "instrumentation works"
    (is (thrown? #?(:clj Exception :cljs :default)
                 (bar {})))

    (is (thrown? #?(:clj Exception :cljs :default)
                 (bar {::x 1 ::y 3})))

    (is (thrown? #?(:clj Exception :cljs :default)
                 (baz {::x 1 ::y 3}))))

  (testing "functions works"
    (is (= (bar {::x 3 ::y 2})
           1))

    (is (= (baz {::x 3 ::y 2 ::a 1})
           "prefix 3"))

    (is (= (baz {::x 3 ::y 2 ::a 1 ::z "suffix"})
           "suffix 3"))))

(deftest suggestions
  (testing "Dependencies"
    (are [x y] (= x y)
      (ms/dependencies foo) #{}
      (ms/dependencies bar) #{}
      (ms/dependencies baz) `#{foo bar}))

  (testing "Suggestions work"
    (is (= (ms/param-suggestions foo)
           {:req #{}, :opt #{}, :req-un #{}, :opt-un #{}}))

    (is (= (ms/param-suggestions bar)
           {:req #{}, :opt #{}, :req-un #{}, :opt-un #{}}))

    (is (= (ms/param-suggestions baz)
           {:req #{::a
                   ::x
                   ::y}
            :opt #{}, :req-un #{}, :opt-un #{}})))

  (testing "param deps"
    (is (= (ms/param-users ::a)
           `#{foo baz}))

    (is (= (ms/param-users ::z)
           `#{baz}))

    (is (= (ms/param-users ::x)
           `#{bar}))))
