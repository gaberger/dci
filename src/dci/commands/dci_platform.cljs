(ns dci.commands.dci-platform
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(def module-version "0.0.5")


(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "Platform Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]
    (.. program
        (command "k8 <command>" "This is a k8 install"))

    (utils/handle-command-default program)

    (.parse program (.-argv js/process))
    
    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      (.-dryrun program) (swap! app-state assoc :dryrun true)
      (.-debug program) (do (swap! app-state assoc :debug true)
                            (js/console.log program)
                            (pprint/pprint @app-state))
      (= (.-args.length program) 0) (.. program
                                        (help #(clojure.string/replace % #"dci-platform" "platform")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (command-handler))
