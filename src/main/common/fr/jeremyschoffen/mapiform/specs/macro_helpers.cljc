(ns fr.jeremyschoffen.mapiform.specs.macro-helpers
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [clojure.spec.alpha :as s])))


(def clj-syms
  "Symbol used to make specs on the clojure side."
  {:fdef 'clojure.spec.alpha/fdef
   :cat 'clojure.spec.alpha/cat
   :keys 'clojure.spec.alpha/keys})


(def cljs-syms
  "Symbol used to make specs on the clojurescript side."
  {:fdef 'cljs.spec.alpha/fdef
   :cat 'cljs.spec.alpha/cat
   :keys 'cljs.spec.alpha/keys})


(defn- make-spec-form
  "Make the `(s/keys ...)` form."
  [{:keys [req opt req-un opt-un]} syms]
  (-> []
      (cond-> (seq req)    (conj :req (vec req))
              (seq opt)    (conj :opt (vec opt))
              (seq req-un) (conj :req-un (vec req-un))
              (seq opt-un) (conj :opt-un (vec opt-un)))
      (->> (cons (:keys syms)))))


(defn- make-defn-spec-form
  "Make the `(fdef x :args ...)` form"
  [n spec syms]
  (let [{:keys [param fn ret]} spec]
    (-> [(:fdef syms) n
         :args (list (:cat syms) :param (make-spec-form param syms))
         :ret ret]
        (cond-> fn (conj :fn fn))
        seq)))


(defn make-defn-spec-form-clj [n spec]
  (make-defn-spec-form n spec clj-syms))


(defn make-defn-spec-form-cljs [n spec]
  (make-defn-spec-form n spec cljs-syms))


(defn sanitize-param [{:keys [req opt req-un opt-un] :as param}]
  (cond-> param
          req (update :req set)
          opt (update :opt set)
          req-un (update :req-un set)
          opt-un (update :req-un set)))


(s/def ::param  (s/map-of #{:req :req-un :opt :opt-un}
                          (s/coll-of keyword?)))
(s/def ::deps (s/coll-of symbol?))
(s/def ::fn any?)
(s/def ::ret any?)


(s/def ::spec-op-opts (s/and (s/map-of #{:deps :param  :fn :ret} any?)
                             (s/keys :opt-un [::deps ::param ::fn  ::ret])))

(s/def ::spec-op-args (s/and (s/cat :name symbol?
                                    :opts (s/* any?))
                             (fn [c]
                               (s/valid? ::spec-op-opts (apply hash-map (:opts c))))))
