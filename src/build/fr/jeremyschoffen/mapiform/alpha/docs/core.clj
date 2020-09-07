(ns fr.jeremyschoffen.mapiform.alpha.docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(u/pseudo-nss
  project)

(def readme-src "fr/jeremyschoffen/mapiform/alpha/docs/pages/README.md.tp")

(defn make-readme! [{wd ::project/working-dir
                     :as conf}]
  (spit (u/safer-path wd "README.md")
        (doc/make-document readme-src
                           conf)))
