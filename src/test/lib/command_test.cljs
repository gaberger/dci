(ns test.lib.command-test
  (:require [schema.core :as s]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [cljs.test :refer-macros [deftest testing is run-tests async]]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [dci.utils.command :as command]
            [dci.utils.core :as utils]
            [dci.components.kubeone :as kubeone]
            [clojure.string :as str]
            [goog.string.format]))



;TODO need to execute kube-one based off platform win/linux/darwin
(deftest spawn-test
  (async done
         (p/let [result (command/run-command "ls -l")]
               (is (= ""
                      result))
               (done))))



