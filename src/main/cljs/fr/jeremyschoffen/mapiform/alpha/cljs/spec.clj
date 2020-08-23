(ns fr.jeremyschoffen.mapiform.alpha.cljs.spec
  (:require
    [clojure.spec.alpha :as s]
    [cljs.analyzer :as ana]
    [fr.jeremyschoffen.mapiform.alpha.specs.macro-helpers :as h]
    [fr.jeremyschoffen.mapiform.alpha.specs.db :as db]))

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
       ~(h/make-defn-spec-form-cljs n sanitized))))


(s/fdef spec-op
        :args ::h/spec-op-args)

(defmacro report
  "Get spec data for `sym`."
  [sym]
  `(db/report '~(ns-qualify &env sym)))

(defmacro dependencies
  "Find the nes of the functions `sym` depends on."
  [sym]
  `(:deps (report ~sym)))


(defmacro param-specs
  "Get the spec of the function named `sym` and the specs of its dependencies."
  [sym]
  `(:spec (report ~sym)))


(defmacro param-suggestions
  "Search the dependencies specs of the function named `sym` to
  find all the potential parameters the function may require."
  [sym]
  `(:suggestions (report ~sym)))
