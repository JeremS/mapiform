(ns fr.jeremyschoffen.mapiform.cljs.spec
  (:require
    [cljs.spec.alpha :include-macros true]
    [fr.jeremyschoffen.mapiform.specs.db]
    [fr.jeremyschoffen.mapiform.specs.db :as db])
  (:require-macros
    [fr.jeremyschoffen.mapiform.cljs.spec]
    [fr.jeremyschoffen.dolly.core :as dolly]))


(dolly/def-clone param-users db/param-users)