(ns fr.jeremyschoffen.mapiform.clj.spec
  (:require
    [clojure.spec.alpha :as s]
    [fr.jeremyschoffen.dolly.core :as dolly]
    [fr.jeremyschoffen.mapiform.specs.macro-helpers :as h]
    [fr.jeremyschoffen.mapiform.specs.db :as db]))

(defn- ns-qualify
  "Qualify symbol s by resolving it or using the current *ns*."
  [s]
  (if-let [ns-sym (some-> s namespace symbol)]
    (or (some-> (get (ns-aliases *ns*) ns-sym)
                str
                (symbol (name s)))
        s)
    (symbol (str (.name *ns*)) (str s))))


(defn- sanitize-deps [deps]
  (->> deps
       (mapv ns-qualify)
       set))


(defn- sanitize-spec [{:keys [deps param fn ret]
                       :or {deps #{}
                            param {}
                            ret 'any?}}]
  (cond-> {:deps (sanitize-deps deps)
           :param (h/sanitize-param param)
           :ret ret}
          fn (assoc :fn fn)))


(defmacro spec-op
  "Declare a spec for a one arg map function."
  {:arglists '([name & {:keys [deps param fn ret]}])}
  [n & {:as spec}]
  (let [sanitized (sanitize-spec spec)
        qualified-name (ns-qualify n)]
    `(do
       (db/add-spec! '~qualified-name '~sanitized)
       ~(h/make-defn-spec-form-clj n sanitized))))

(s/fdef spec-op
        :args ::h/spec-op-args)


(defmacro report
  "Get spec data for `sym`."
  [sym]
  `(db/report '~(ns-qualify sym)))

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




(dolly/def-clone param-users db/param-users)
