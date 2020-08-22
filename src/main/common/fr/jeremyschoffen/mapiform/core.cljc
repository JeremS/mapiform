(ns fr.jeremyschoffen.mapiform.core
  (:require
    [medley.core :as medley]))


(defn- check-kfs [kfs]
  (when-not (even? (count kfs))
    (throw (ex-info "Expected even number of arguments." {}))))


(defn wrap-rf [f]
  (fn [m [k f']]
    (f m k f')))


(defn reduce-kvs [f m kvs]
  (check-kfs kvs)
  (reduce (wrap-rf f) m (partition 2 kvs)))


(defn- assoc-computed-1 [m k f]
  (assoc m k (f m)))


(defn- ensure-computed-1 [m k f]
  (if (contains? m k)
    m
    (assoc m k (f m))))


(defn- ensure-1 [m k v]
  (ensure-computed-1 m k (constantly v)))


(defn- augment-computed-1
  [m k f]
  (let [defaults (get m k)
        res (f m)]
    (assoc m k (medley/deep-merge defaults res))))


(defn- augment-1 [m k v]
  (augment-computed-1 m k (constantly v)))


(defn assoc-computed [m & kfs]
  (reduce-kvs assoc-computed-1 m kfs))


(defn ensure-computed [m & kvs]
  (reduce-kvs ensure-computed-1 m kvs))


(defn ensure-v [m & kvs]
  (reduce-kvs ensure-1 m kvs))


(defn augment-computed [m & kfs]
  (reduce-kvs augment-computed-1 m kfs))


(defn augment-v [m & kvs]
  (reduce-kvs augment-1 m kvs))


(defn thread-fns
  "Function serving a similar purpose than the `->` macro. It will  thread a value `v` through a sequence of
  functions `fns` the result of one function application becoming the argument of the next.

  Args:
  - `v`: the value to be threaded
  - `fns`: 1 argument functions that will be applied."
  [v & fns]
  (reduce (fn [acc f]
            (f acc))
          v
          fns))


(defn perform!
  ([f!]
   (fn [v]
     (perform! v f!)))
  ([v f!]
   (f! v)
   v))
