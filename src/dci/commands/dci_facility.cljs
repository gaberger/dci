(ns dci.commands.dci-facility
  (:require [commander]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [cljs.pprint :as pprint]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(def module-version "0.0.5")

(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "Facility Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]

    (.. program
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                    (api/print-facilities (keyword (.-provider program))))))

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
                                        (help #(clojure.string/replace % #"dci-facility" "facility")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (command-handler))


