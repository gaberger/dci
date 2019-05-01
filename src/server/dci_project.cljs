(ns server.dci-project
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.core.async :refer [chan put! take! >! <! buffer
                                     dropping-buffer sliding-buffer timeout close! alts!] :as async]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [clojure.string :as str]
            [server.dci-model :as model :refer [IServer]]
            [lib.packet :as packet :refer [PacketServer]]
            [utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defmulti command-actions identity)
(defmethod command-actions :packet [& args]
  (condp = (second args)
    :print-projects    (model/print-projects (PacketServer.))
    :create-project   (model/create-project (PacketServer.) (nnext args))
    :delete-project   (model/delete-project (PacketServer.) (nnext args))
    :get-project-name (model/get-project-name (PacketServer.) (nnext args))
    (println "Error: unknown command" (second args))))


(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Project Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    )]

    (.. program
        (description "Switch Project ID")
        (command "change <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (p/let [result (command-actions (keyword (.-provider program)) :get-project-name project-id)
                          name (-> result :body :name)]
                        (utils/update-project-id project-id name))
                        (println "Switching to Project" project-id))))

    (.. program
        (description "List Projects")
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                  (command-actions  (keyword (.-provider program)) :print-projects))))

    (.. program
        (description "Create Project")
        (command "create <name>")
        (action (fn [name]
                  (when (.-debug program) (utils/set-debug!))
                  (command-actions  (keyword (.-provider program)) :create-project name))))

    (.. program
        (description "Delete Project")
        (command "delete <project-id>")
        (action (fn [projectId]
                  (when (.-debug program) (utils/set-debug!))
                  (command-actions  (keyword (.-provider program)) :delete-project projectId))))


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
              (help #(clojure.string/replace % #"dci-project" "project"))
              ))))

(defn main! []
  (command-handler))


