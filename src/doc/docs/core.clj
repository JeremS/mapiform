(ns docs.core
  (:require
    [fr.jeremyschoffen.textp.alpha.doc.core :as doc]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]))

(def readme-src "docs/pages/README.md.tp")

(defn make-readme! [{wd :project/working-dir
                     :as conf}]
  (spit (u/safer-path wd "README.md")
        (doc/make-document readme-src
                           conf)))
