(ns server.dci-server
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [cljs.core.async :refer [chan put! take! >! <! buffer
                                     dropping-buffer sliding-buffer timeout close! alts!] :as async]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [clojure.string :as str]
            [server.dci-model :as model :refer [IServer list-servers create-server delete-server get-project-name ]]
            [lib.packet :as packet :refer [PacketServer]]
            [utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)
(defmulti command-actions identity)
(defmethod command-actions :packet [& args]
  (condp = (second args)
    :list-servers (list-servers (PacketServer.) (nnext args))
    :get-project-name (get-project-name (PacketServer.) (nnext args))
    :delete-server (delete-server (PacketServer.) (nnext args))
    :create-server (create-server (PacketServer.) (nnext args))
    :default (println "Error: unknown command" (second args))))

(defn create-handler [args]
  (let [[command project-id hostname plan facilities operating-system] args]
    (println "calling create-handler" (count args))
    #_(dump-object (last args))))

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Server Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    )]

    (if (utils/projectid?)
      (.. program
          (description "List Bare Metal Cloud Servers. Requires project-id")
          (command "list")
          (option "-p --project-id <project-id>" "Select Project")
          (action (fn [cmd]
                    (when (.-debug program) (swap! app-state assoc :debug true))
                    (go
                      (let [[id name] (if (some? (.-projectId cmd))
                                        (let [project-id   (.-projectId cmd)
                                              chan         (command-actions (keyword (.-provider program))
                                                            :get-project-name project-id)
                                              project-name (<! chan)]
                                          [project-id project-name])
                                        (utils/get-project-id-state))]
                        (when-not (:json @app-state)
                          (println "Using Project:" name "\nID:" id))
                        (command-actions (keyword (.-provider program)) :list-servers id))
                        ))))

    (.. program
        (description "List Bare Metal Cloud Servers. Requires project-id")
        (command "list <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (command-actions (keyword (.-provider program)) :list-servers project-id))))
      )

    (if (utils/projectid?)
         (.. program
          (command "create <hostname> [plan] [facilities] [os]")
          (description "Create Bare Metal Cloud Server.")
          (action (fn [hostname plan facilities os]
                    (let [[project-id _] (utils/get-project-id-state)
                          hostname'      (str hostname)
                          plan'          (or plan  "baremetal_0")
                          facilities'    (or facilities ["ewr1"] )
                          os'            (or os "ubuntu_16_04")
                          args           {:id       project-id  :hostname         hostname' :plan plan'
                                          :facility facilities' :operating_system os'}]
                      (when (.-debug program) (swap! app-state assoc :debug true))
                      (command-actions (keyword (.-provider program))
                                       :create-server args)))))
         (.. program
            (command "create <project-id> <hostname> [plan] [facilities] [os]")
            (description "Create Bare Metal Cloud Server.")
            (action (fn [project-id hostname plan facilities os]
                      (let [hostname'   (str hostname)
                            plan'       (or plan  "baremetal_0")
                            facilities' (or facilities ["ewr1"] )
                            os'         (or os "ubuntu_16_04")
                            args        {:id       project-id  :hostname         hostname' :plan plan'
                                         :facility facilities' :operating_system os'}]
                       (when (.-debug program) (swap! app-state assoc :debug true))
                                         (command-actions (keyword (.-provider program))
                                         :create-server args))))))

    (.. program
        (command "delete <device-id>")
        (description "Delete Bare Metal Cloud Server. Requires device-id")
        (action (fn [device-id options]
                  (command-actions  (keyword (.-provider program)) :delete-server device-id))))


    (.parse program (.-argv js/process))
    (when (.-json program) (swap! app-state assoc :json true))
    (cond (= (.-args.length program) 0)
          (.. program 
              (help #(clojure.string/replace % #"dci-server" "server"))
              ))))

(defn main! []
  (utils/get-api-token)
  (command-handler))
