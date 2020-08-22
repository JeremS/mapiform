(ns fr.jeremyschoffen.mapiform.specs.db
  (:refer-clojure :exclude [meta])
  (:require
    #?@(:clj [[clojure.spec.alpha :as s]]
        :cljs [[cljs.spec.alpha :as s :include-macros true]])))


(def specs-store (atom {}))
(def meta-store (atom {}))

(defn get-spec [sym]
  (get @specs-store sym))


(defn add-spec! [fn-name spec]
  (swap! specs-store assoc fn-name spec))


(defn meta [x]
  (get @meta-store x))


(defn update-meta [db id f & args]
  (apply update db id f args))


(defn update-meta! [id f & args]
  (apply swap! meta-store update-meta id f args))


(defn get-deps* [specs-map sym]
  (get-in specs-map [sym :deps] #{}))


(defn- merge-param-specs* [param-specs type]
  (into (sorted-set)
        (mapcat type)
        param-specs))


(defn- merge-param-specs [param-specs]
  {:req (merge-param-specs* param-specs :req)
   :opt (merge-param-specs* param-specs :opt)
   :req-un (merge-param-specs* param-specs :req-un)
   :opt-un (merge-param-specs* param-specs :opt-un)})


(defn get-param-specs-suggestions* [spec-map sym]
  (let [deps (get-deps* spec-map sym)]
    (with-meta (->> deps
                    (into [] (comp (map (partial get spec-map))
                                   (map :param)))
                    merge-param-specs)
               {:details (select-keys spec-map deps)})))


(defn get-param-specs-suggestions [sym]
  (get-param-specs-suggestions* @specs-store sym))


(defn get-param-specs [sym]
  {:spec (get-spec sym)
   :transitive-suggestions (get-param-specs-suggestions sym)})


(defn requirer
  "Return a set of all function depending on that keyword."
  [kw]
  (into #{}
        (keep (fn [[f spec]]
                (let [param-spec (:param spec)
                      {:keys [req opt]} param-spec]
                  (when (or (contains? req kw)
                            (contains? opt kw))
                    f))))
        @specs-store))






(comment
  (defn toto [x] x)

  (update-meta {} toto assoc :name `toto)







  (update-meta! toto assoc :name `toto)

  @meta-store)