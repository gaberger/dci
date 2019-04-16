(ns server.dci-server
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [cljs.core.async :refer [chan put! take! >! <! buffer
                                     dropping-buffer sliding-buffer timeout close! alts!] :as async]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [clojure.string :as str]
            [server.dci-model :as model :refer [IServer list-servers create-server delete-server]]
            [lib.packet :as packet :refer [PacketServer]]
            [dci.main :refer [app-state]]))

(enable-console-print!)

(defn get-api-token []
  (if (nil? (.-APIKEY (.-env js/process)))
    (do (println "Error: Set APIKEY environmental variable")
      (.exit js/process 1))
    (swap! app-state assoc :apikey (.-APIKEY (.-env js/process)))))

(defn dump-object [obj]
  (println "###########")
  (println (.inspect util obj)))


(defmulti command-actions identity)
(defmethod command-actions :packet [_ command args ]
  (condp = command
    :list-servers (list-servers (PacketServer.) args)
    :delete-server (delete-server (PacketServer.) args)
    :create-server (create-server (PacketServer.) args)
    (println "Error: unknown command" command)))


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
    (.. program
        (description "List Bare Metal Cloud Servers. Requires project-id")
        (command "list <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (command-actions (keyword (.-provider program)) :list-servers project-id))))
    (.. program
        (command "create <project-id> <hostname> [plan] [facilities] [os]")
        (description "Create Bare Metal Cloud Server. Requires project-id, hostname")
        (action (fn [project-id hostname plan facilities os]
                  (let [hostname'   (str hostname)
                        plan'       (if plan plan "baremetal_0")
                        facilities' (if facilities facilities ["ewr1"] )
                        os'         (if os os "ubuntu_16_04")
                        args {:id project-id :hostname hostname' :plan plan' :facility facilities' :operating_system os'}]
                    (when (.-debug program) (swap! app-state assoc :debug true))
                    (command-actions (keyword (.-provider program))
                                                  :create-server args)))))
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
  (get-api-token)
  (command-handler))
