(ns server.dci-service)
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [cljs.core.async :refer [chan put! take! >! <! buffer
                                     dropping-buffer sliding-buffer timeout close! alts!] :as async]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [clojure.string :as str]
            [server.dci-model :as model :refer [IServer delete-project create-project list-servers create-server delete-server list-projects get-project-name]]
            [lib.packet :as packet :refer [PacketServer]]
            [utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defmulti command-actions identity :default :default)
(defmethod command-actions :packet [& args]
  (condp = (second args)
    :create-service (create-service (PacketServer.))
    :default          (println "Error: unknown command" (second args))))


(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Service Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    )]

    (.. program
        (description "Create Service")
        (command "create <name>")
        (action (fn [name]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (let [chan (command-actions (keyword (.-provider program)) :create-service name)]
                    (go
                      (let [project-name (<! chan)]
                        (utils/update-project-id project-id project-name))
                        (println "Switching to Project" project-id))))))

    (.parse program (.-argv js/process))
    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      :else            (swap! app-state assoc :output :table)
      )

    (when (.-debug program) (do
                              (swap! app-state assoc :debug true)
                              (js/console.log program)
                              (pprint/pprint @app-state)))

    (cond (= (.-args.length program) 0)
          (.. program 
              (help #(clojure.string/replace % #"dci-service" "service"))
              ))))

(defn main! []
  (command-handler))
