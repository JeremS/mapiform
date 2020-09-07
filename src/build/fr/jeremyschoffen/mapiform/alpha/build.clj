(ns fr.jeremyschoffen.mapiform.alpha.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.mapiform.alpha.docs.core :as docs]
    [build :refer [token]]))


(u/pseudo-nss
  build
  build.jar
  git
  git.commit
  maven
  maven.credentials
  maven.pom
  project
  project.deps
  project.license
  versioning)


;;----------------------------------------------------------------------------------------------------------------------
;; Config
;;----------------------------------------------------------------------------------------------------------------------
(def base-conf {::project-name "mapifom"
                ::project/author "Jeremy Schoffen"
                ::project/git-url "https://github.com/JeremS/mapiform"

                ::maven/group-id 'fr.jeremyschoffen
                ::versioning/major :alpha
                ::versioning/scheme mbt-defaults/git-distance-scheme

                ::maven/server mbt-defaults/clojars
                ::maven/credentials {::maven.credentials/user-name "jeremys"
                                     ::maven.credentials/password token}

                ::project/licenses [{::project.license/name "Eclipse Public License - v 2.0"
                                     ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                     ::project.license/distribution :repo
                                     ::project.license/file (u/safer-path "LICENSE")}]})


(defn clj-jar-out
  "Directory specific for the jar containing the clj part of the code."
  [{out ::project/output-dir}]
  (u/safer-path out "clj"))


(defn cljs-jar-out
  "Directory specific for the jar containing the cljs part of the code."
  [{out ::project/output-dir}]
  (u/safer-path out "cljs"))


(defn maven-pom-path
  "Pom goes where jar goes."
  [{jar-out ::build/jar-output-dir}]
  (u/safer-path jar-out "pom.xml"))


(defn dissoc-paths
  "We get rid of the default paths for building, each artefact merges their own."
  [conf]
  (update conf ::project/deps dissoc :paths))



(def conf-clj (-> base-conf
                  (merge {::project.deps/aliases [:build/clojure]
                          ::build/jar-output-dir (mbt-defaults/config-calc clj-jar-out ::project/output-dir)
                          ::maven.pom/path (mbt-defaults/config-calc maven-pom-path ::build/jar-output-dir)})
                  mbt-defaults/config
                  dissoc-paths
                  (->> (into (sorted-map)))))


(def conf-cljs (-> base-conf
                   (merge {::project/name "mapiform-cljs"
                           ::project.deps/aliases [:build/cljs]
                           ::build/jar-output-dir (mbt-defaults/config-calc cljs-jar-out ::project/output-dir)
                           ::maven.pom/path (mbt-defaults/config-calc maven-pom-path ::build/jar-output-dir)})
                   mbt-defaults/config
                   dissoc-paths
                   (->> (into (sorted-map)))))

;;----------------------------------------------------------------------------------------------------------------------
;; Docs generation
;;----------------------------------------------------------------------------------------------------------------------
(defn merge-last-version [conf]
  (-> conf-clj
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-last-version
                        ::project/version mbt-defaults/versioning-project-version)
      (select-keys #{::versioning/version ::project/version})
      (->> (merge conf))))


(defn coords-clj []
  (-> conf-clj
      merge-last-version
      (u/assoc-computed ::project/maven-coords mbt-defaults/deps-make-maven-coords
                        ::project/git-coords mbt-defaults/deps-make-git-coords)
      (select-keys #{::project/maven-coords
                     ::project/git-coords})))


(defn coords-clj->cljs [clj-coords]
  (let [clj-name (mbt-core/deps-symbolic-name conf-clj)
        mvn (get-in clj-coords [::project/maven-coords clj-name])
        git (get-in clj-coords [::project/git-coords clj-name])
        n (mbt-core/deps-symbolic-name conf-cljs)]
    {::project/maven-coords {n mvn}
     ::project/git-coords {n git}}))


(defn make-readme! [conf]
  (let [clj-maven-coords (coords-clj)
        cljs-maven-coords (coords-clj->cljs (coords-clj))]
    (docs/make-readme! (merge conf {:clj-coords clj-maven-coords
                                    :cljs-coords cljs-maven-coords}))))


(defn make-docs! [conf]
  (-> conf
      (assoc-in [::git/commit! ::git.commit/message] "Adding docs.")
      (mbt-defaults/generate-then-commit!
        (u/do-side-effect! make-readme!))))


;;----------------------------------------------------------------------------------------------------------------------
;; Bump
;;----------------------------------------------------------------------------------------------------------------------
(defn bump-project!
  "Creates a new tag marking a new version."
  []
  (-> conf-clj
      (u/assoc-computed ::versioning/version mbt-defaults/versioning-next-version
                        ::project/version mbt-defaults/versioning-project-version)
      (u/do-side-effect! mbt-defaults/versioning-tag-new-version!)
      (u/do-side-effect! make-docs!)))


;;----------------------------------------------------------------------------------------------------------------------
;; Building and deploying jars
;;----------------------------------------------------------------------------------------------------------------------
(defn build-clj! []
  (-> conf-clj
      merge-last-version
      mbt-defaults/versioning-update-scm-tag
      (u/do-side-effect! mbt-defaults/maven-sync-pom!)
      (u/do-side-effect! mbt-defaults/build-jar!)))


(defn add-clj-coords [conf clj-conf]
  (let [clj-coords (mbt-defaults/deps-make-maven-coords clj-conf)]
    (update-in conf [::project/deps :deps] merge clj-coords)))


(defn build-cljs! [clj-conf]
  (let [common (select-keys clj-conf
                            #{::project/version
                              ::maven/scm})]
    (-> conf-cljs
        (merge common)
        (add-clj-coords clj-conf)
        (u/do-side-effect! mbt-defaults/maven-sync-pom!)
        (u/do-side-effect! mbt-defaults/build-jar!))))


(defn build-project! []
  (-> (build-clj!)
      (build-cljs!)))


(defn deploy! []
  (do (-> conf-clj
          merge-last-version
          mbt-defaults/maven-deploy!)
      (-> conf-cljs
          merge-last-version
          mbt-defaults/maven-deploy!)))



(st/instrument
  `[bump-project!
    mbt-defaults/versioning-last-version
    mbt-defaults/versioning-project-version
    mbt-defaults/deps-make-maven-coords
    mbt-defaults/deps-make-git-coords
    mbt-defaults/versioning-tag-new-version!
    mbt-defaults/build-jar!
    mbt-defaults/maven-install!
    mbt-defaults/maven-deploy!])


(comment
  (make-readme! conf-clj)
  (bump-project!)
  (build-project!)
  (mbt-core/clean! conf-clj)


  (deploy!))
