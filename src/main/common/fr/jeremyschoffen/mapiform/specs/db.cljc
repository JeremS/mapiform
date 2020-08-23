(ns fr.jeremyschoffen.mapiform.specs.db
  (:refer-clojure :exclude [meta])
  (:require
    #?@(:clj [[clojure.spec.alpha :as s]]
        :cljs [[cljs.spec.alpha :as s :include-macros true]])))


(def specs-store (atom {}))


(defn add-spec! [fn-name spec]
  (swap! specs-store assoc fn-name spec))


(defn- merge-param-specs* [param-specs type]
  (into (sorted-set)
        (mapcat type)
        param-specs))


(defn- merge-param-specs [param-specs]
  {:req (merge-param-specs* param-specs :req)
   :opt (merge-param-specs* param-specs :opt)
   :req-un (merge-param-specs* param-specs :req-un)
   :opt-un (merge-param-specs* param-specs :opt-un)})


(defn get-deps* [db sym]
  (get-in db [sym :deps] #{}))


(defn get-suggestions* [db sym]
  (let [deps (get-deps* db sym)]
    (->> deps
         (into [] (comp (map (partial get db))
                        (map :param)))
         merge-param-specs)))


(defn report [sym]
  (let [db @specs-store
        deps (get-deps* db sym)]
    {:deps deps
     :spec (get db sym)
     :suggestions (get-suggestions* db sym)
     :suggestions-sources (select-keys db deps)}))


(defn param-users
  "Return a set of all functions depending on the keyword `kw`."
  [kw]
  (into #{}
        (keep (fn [[f spec]]
                (when (some #(contains? % kw)
                            (-> spec :param vals))
                  f)))
        @specs-store))
