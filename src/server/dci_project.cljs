(ns server.dci-project
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [cljs.core.async :refer [chan put! take! >! <! buffer
                                     dropping-buffer sliding-buffer timeout close! alts!] :as async]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [clojure.string :as str]
            [server.dci-model :as model :refer [IServer list-servers create-server delete-server list-projects get-project-name]]
            [lib.packet :as packet :refer [PacketServer]]
            [utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defmulti command-actions identity :default :default)
(defmethod command-actions :packet [& args]
  (condp = (second args)
    :list-projects (list-projects (PacketServer.))
    :get-project-name (get-project-name (PacketServer.) (nnext args))
    :default (println "Error: unknown command" (second args))))


(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Project Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    )]

    (.. program
        (description "Switch Project ID")
        (command "change <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (let [chan (command-actions (keyword (.-provider program)) :get-project-name project-id)]
                    (go
                      (let [project-name (<! chan)]
                        (utils/update-project-id project-id project-name))
                        (println "Switching to Project" project-id))))))

    (.. program
        (description "List Projects")
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                    (command-actions  (keyword (.-provider program)) :list-projects))))


    (.parse program (.-argv js/process))
    (when (.-json program) (swap! app-state assoc :json true))
    (cond (= (.-args.length program) 0)
          (.. program 
              (help #(clojure.string/replace % #"dci-project" "project"))
              ))))

(defn main! []
  (utils/get-api-token)
  (command-handler))


