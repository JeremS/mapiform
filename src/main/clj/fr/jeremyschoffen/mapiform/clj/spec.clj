(ns fr.jeremyschoffen.mapiform.clj.spec
  (:require
    [clojure.spec.alpha :as s]
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
       (db/update-meta! ~n assoc :name '~qualified-name)
       ~(h/make-defn-spec-form-clj n sanitized))))

(s/fdef spec-op
        :args ::h/spec-op-args)


(defmacro param-suggestions
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(db/get-param-specs-suggestions '~(ns-qualify sym)))


(defmacro param-specs
  "Search the function spec and the declared dependencies specs transitively to
  find all the potential params the function may require."
  [sym]
  `(db/get-param-specs '~(ns-qualify sym)))

(comment)
