(ns dci.commands.dci-organization
  (:require [commander]
            [util]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [dci.drivers.packet]
            [dci.drivers.interfaces :as api]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Project Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --n" "Output to JSON")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]

    (.. program
        (description "List Organizations")
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                  (api/print-organizations (keyword (.-provider program))))))

    (.parse program (.-argv js/process))
    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      :else            (swap! app-state assoc :output :table))

    (when (.-debug program) (do
                              (swap! app-state assoc :debug true)
                              (js/console.log program)
                              (pprint/pprint @app-state)))

    (cond (= (.-args.length program) 0)
          (.. program
              (help #(clojure.string/replace % #"dci-organiztion" "organization"))))))

(defn main! []
  (command-handler))


