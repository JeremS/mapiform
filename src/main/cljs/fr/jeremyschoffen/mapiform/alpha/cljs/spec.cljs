(ns fr.jeremyschoffen.mapiform.alpha.cljs.spec
  (:require
    [cljs.spec.alpha :include-macros true]
    [fr.jeremyschoffen.mapiform.alpha.specs.db :as db])
  (:require-macros
    [fr.jeremyschoffen.mapiform.alpha.cljs.spec]
    [fr.jeremyschoffen.dolly.core :as dolly]))

(dolly/def-clone get-report db/report)
(dolly/def-clone param-users db/param-users)