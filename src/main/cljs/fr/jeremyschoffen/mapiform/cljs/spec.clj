(ns fr.jeremyschoffen.mapiform.cljs.spec
  (:require
    [clojure.spec.alpha :as s]
    [cljs.analyzer :as ana]
    [fr.jeremyschoffen.mapiform.specs.macro-helpers :as h]
    [fr.jeremyschoffen.mapiform.specs.db :as db]))

;;----------------------------------------------------------------------------------------------------------------------
;; from cljs.spec.alpha
(defn- ->sym
  "Returns a symbol from a symbol or var"
  [x]
  (if (map? x)
    (:name x)
    x))

(defn- ns-qualify
  "Qualify symbol s by resolving it or using the current *ns*."
  [env s]
  (if (namespace s)
    (->sym (binding [ana/*private-var-access-nowarn* true]
             (ana/resolve-var env s)))
    (symbol (str ana/*cljs-ns*) (str s))))
;;----------------------------------------------------------------------------------------------------------------------


(defn- sanitize-deps [env deps]
  (->> deps
       (mapv (partial ns-qualify env))
       set))


(defn- sanitize-spec [env {:keys [deps param fn ret]
                           :or {deps #{}
                                param {}
                                ret 'any?}}]
  (cond-> {:deps (sanitize-deps env deps)
           :param (h/sanitize-param param)
           :ret ret}
          fn (assoc :fn fn)))


(defmacro spec-op
  "Declare a spec for a one arg map function."
  {:arglists '([name & {:keys [deps param fn ret]}])}
  [n & {:as spec}]
  (let [sanitized (sanitize-spec &env spec)
        qualified-name (ns-qualify &env n)]
    `(do
       (db/add-spec! '~qualified-name '~sanitized)
       (db/update-meta! ~n assoc :name '~qualified-name)
       ~(h/make-defn-spec-form-cljs n sanitized))))


(s/fdef spec-op
        :args ::h/spec-op-args)


(defmacro param-suggestions
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(db/get-param-specs-suggestions '~(ns-qualify &env sym)))


(defmacro param-specs
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(db/get-param-specs '~(ns-qualify &env sym)))