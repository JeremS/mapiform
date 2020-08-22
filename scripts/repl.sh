#!/usr/bin/env bash

clojure -A:common-part:clojure-part:cljs-part:clj:cljs:dev:nrepl:piggie:test -m nrepl.cmdline --middleware "[cider.piggieback/wrap-cljs-repl]"


