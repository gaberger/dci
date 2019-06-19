(ns dci.commands.dci-service-install
  (:require [commander]
            [util]
            [ip-utils :as ip]
            [child_process :as proc]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [cljs.nodejs.shell]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(def module-version "0.0.4")


;(def kubeone-commands ["kubeone-linux" "install" "<(dci service install kubeone cluster-test-1 192.168.0.0/16 172.16.0.0.12)]")


(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "Service Install Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]


    (.. program
        (command "k8 <service> <cluster-network> <service-subnet>")
        (option "-s --sshUsername [user]")
        (option "-k --sshprivKeyFile [key]")

        (action (fn [service cluster-network service-subnet cmd]
                  #_(let [cp (proc/spawn "./dependencies/kubeone-linux" ["--help"] {})]
                      (.on (.-stdout cp) "data" (fn [data] (println (.toString data))))
                      (when (.-debug program) (swap! app-state assoc :debug true)))

                  (p/let [_ (utils/set-env "PACKET_AUTH_TOKEN" (utils/get-env "APIKEY"))
                          _ (utils/set-env "PACKET_PROJECT_ID" (utils/get-env "PROJECT_ID"))
                          config  (kubeone/create-kubeone-config "packet" cluster-network service-subnet)
                          result (command/run-command
                                  (goog.string.format "./dependencies/kubeone-linux install <(echo \"%s\") -b %s" config service))]))))

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
                                        (help #(clojure.string/replace % #"dci-cluster" "cluster")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (command-handler))

