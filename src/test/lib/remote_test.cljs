(ns test.lib.remote-test
  (:require  [dci.drivers.packet :as packet]
             [dci.state :refer [app-state]]
             [dci.utils.core :as utils]
             [clojure.string :as str]
             [dci.drivers.packet :as api]
             [schema.core :as s]
             [taoensso.timbre :as timbre
              :refer-macros [log  trace  debug  info  warn  error  fatal  report
                             logf tracef debugf infof warnf errorf fatalf reportf
                             spy get-env]]
             [martian.core :as martian]
             [martian.test :as martian-test]
             [cljs.test :refer-macros [deftest testing is run-tests async]]
             [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
             [dci.utils.command :as command]))

(enable-console-print!)

(deftest login-test
  (let [response (chan)
        host     (command/host-factory "localhost" 22 "tes" "test")
        commands {:commands ["cat /etc/os-release"]}
        call     (clj->js (merge host commands))]
    (command/call-ssh response call)
    (async done
           (go
             (let [result (str/split-lines (<! response))]
               (is (= ""
                      result))
               (done))))))


