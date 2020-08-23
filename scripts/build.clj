(ns build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [docs.core :as docs]))


(st/instrument
  `[mbt-core/deps-make-coord
    mbt-defaults/build-jar!
    mbt-defaults/install!])


(def specific-conf (sorted-map
                     :project-name "mapifom"
                     :project/author "Jeremy Schoffen"
                     :maven/group-id 'fr.jeremyschoffen
                     :versioning/major :alpha
                     :versioning/scheme mbt-defaults/git-distance-scheme

                     :project/licenses [{:project.license/name "Eclipse Public License - v 2.0"
                                         :project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                         :project.license/distribution :repo
                                         :project.license/file (u/safer-path "LICENSE")}]))


(def conf-clj (->  specific-conf
                   (assoc
                     :project/output-dir (u/safer-path "target" "clj")
                     :project.deps/aliases [:common-part :clojure-part])
                   mbt-defaults/make-conf
                   (u/assoc-computed :project/deps mbt-core/deps-get)))


(def conf-cljs (-> specific-conf
                   (assoc :project/name "mapiform-cljs"
                          :project/output-dir (u/safer-path "target" "cljs")
                          :project.deps/aliases [:cljs-part])
                   mbt-defaults/make-conf
                   (u/assoc-computed :project/deps mbt-core/deps-get)))



(defn make-readme! [{v :project/version
                     :as conf}]
  (let [clj-maven-coords (-> conf-clj
                             (assoc :project/version v)
                             mbt-core/deps-make-coord)
        cljs-maven-coords (-> conf-cljs
                              (assoc :project/version v)
                              mbt-core/deps-make-coord)]
    (docs/make-readme! (merge conf {:clj-coords clj-maven-coords
                                    :cljs-coords cljs-maven-coords}))))


(defn make-docs! [conf]
  (make-readme! conf))


(defn new-milestone! [conf]
  (-> conf
      (mbt-defaults/generate-before-bump! (u/side-effect! make-docs!))
      mbt-defaults/bump-tag!))


(defn build-base-jar! []
  (-> conf-clj
      (u/side-effect! mbt-defaults/build-jar!)
      (u/side-effect! mbt-defaults/install!)))


(defn add-clj-coords [{v :project/version :as conf}]
  (let [clj-coords (-> conf-clj
                       (assoc :project/version v)
                       mbt-core/deps-make-coord)]
    (update-in conf [:project/deps :deps] merge clj-coords)))


(defn build-cljs-jar! []
  (-> conf-cljs
      (assoc :project/version (mbt-defaults/current-project-version conf-clj))
      add-clj-coords
      (u/side-effect! mbt-defaults/build-jar!)
      (u/side-effect! mbt-defaults/install!)))

(comment
  (new-milestone! conf-clj)

  (mbt-core/clean! conf-clj)
  (build-base-jar!)
  (build-cljs-jar!))