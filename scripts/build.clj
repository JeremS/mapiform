(ns build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [docs.core :as docs]))

(u/pseudo-nss
  build
  build.jar
  maven
  maven.pom
  project
  project.deps
  project.license
  versioning)


(st/instrument
  `[mbt-core/deps-make-coord
    mbt-defaults/versioning-tag-new-version!
    mbt-defaults/build-jar!
    mbt-defaults/maven-install!])

;;----------------------------------------------------------------------------------------------------------------------
;; Config
;;----------------------------------------------------------------------------------------------------------------------
(def base-conf {::project-name "mapifom"
                ::project/author "Jeremy Schoffen"
                ::maven/group-id 'fr.jeremyschoffen
                ::versioning/major :alpha
                ::versioning/scheme mbt-defaults/git-distance-scheme

                ::project/licenses [{::project.license/name "Eclipse Public License - v 2.0"
                                     ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                     ::project.license/distribution :repo
                                     ::project.license/file (u/safer-path "LICENSE")}]})

(defn clj-jar-out [{out ::project/output-dir}]
  (u/safer-path out "clj"))


(defn cljs-jar-out [{out ::project/output-dir}]
  (u/safer-path out "cljs"))

(defn dissoc-paths [conf]
  (update conf ::project/deps dissoc :paths))


(def conf-clj (-> base-conf
                  (merge {::project.deps/aliases [:build/clojure]
                          ::build/jar-output-dir (mbt-defaults/config-calc clj-jar-out ::project/output-dir)
                          ::maven.pom/dir (mbt-defaults/config-calc clj-jar-out ::project/output-dir)})
                  mbt-defaults/config-make
                  dissoc-paths
                  (->> (into (sorted-map)))))




(def conf-cljs (-> base-conf
                   (merge {::project/name "mapiform-cljs"
                           ::project.deps/aliases [:build/cljs]
                           ::build/jar-output-dir (mbt-defaults/config-calc cljs-jar-out ::project/output-dir)
                           ::maven.pom/dir (mbt-defaults/config-calc cljs-jar-out ::project/output-dir)})
                   mbt-defaults/config-make
                   dissoc-paths
                   (->> (into (sorted-map)))))

(defn artefact-coords [v]
  (-> conf-clj (assoc ::project/version v) mbt-core/deps-make-coord))

(defn artefact-coords-cljs [v]
  (-> conf-cljs (assoc ::project/version v) mbt-core/deps-make-coord))
;;----------------------------------------------------------------------------------------------------------------------
;; Doc gen and creating new git tags
;;----------------------------------------------------------------------------------------------------------------------
(defn next-version [conf]
  (let [initial (mbt-defaults/versioning-initial-version conf)
        next-v (mbt-defaults/versioning-next-version conf)]
    (-> next-v
        (cond-> (not= initial next-v)
                (update :distance inc))
        str)))


(defn make-readme! [{v ::project/version :as conf}]
  (let [clj-maven-coords (artefact-coords v)
        cljs-maven-coords (artefact-coords-cljs v)]
    (docs/make-readme! (merge conf {:clj-coords clj-maven-coords
                                    :cljs-coords cljs-maven-coords}))))


(defn make-docs! [conf]
  (make-readme! conf))


(defn make-docs-then-tag! [conf]
  (-> conf
      (u/assoc-computed ::versioning/version next-version
                        ::project/version (comp str ::versioning/version))
      (mbt-defaults/build-before-bump! (u/do-side-effect! make-docs!))
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)))


;;----------------------------------------------------------------------------------------------------------------------
;; Building and deploying jars
;;----------------------------------------------------------------------------------------------------------------------
(defn build-base-jar! [conf]
  (-> conf
      (update ::project/deps dissoc :paths)
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)))


(defn add-clj-coords [{v ::project/version :as conf}]
  (let [clj-coords (artefact-coords v)]
    (update-in conf [::project/deps :deps] merge clj-coords)))


(defn build-cljs-jar! [conf]
  (-> conf
      (select-keys #{::project/version})
      (merge conf-cljs)
      add-clj-coords
      (u/do-side-effect! mbt-defaults/build-jar!)
      (u/do-side-effect! mbt-defaults/maven-install!)))

(defn new-milestone! []
  (-> conf-clj
      make-docs-then-tag!
      build-base-jar!
      build-cljs-jar!))

(comment
  (new-milestone!)


  (mbt-core/clean! conf-clj))
