(ns dci.commands.dci-platform-k8
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
            [dci.utils.command :as command]
            [dci.components.kubeone :as kubeone]
            [dci.state :refer [app-state]]))

(def module-version "0.0.5")

(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "K8 Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]
;;TODO CHeck for existing project and server liveness
    ;;TODO Create project if doesn't exist?/ in service deploy?
    ;;TODO Combine service/deploy with platform install?


    (.. program
        (command "config <cluster-name> <cluster-network> <service-subnet>")
        (action (fn [service cluster-network service-subnet cmd]
                  (p/let [config  (kubeone/create-kubeone-config "packet" service cluster-network service-subnet)]
                    (println config)))))

    (.. program
        (command "install <config>")
        (option "-s --sshUsername [user]")
        (option "-k --sshprivKeyFile [key]")
        (action (fn [config cmd]
                  (utils/set-env "PACKET_AUTH_TOKEN" (utils/get-env "APIKEY"))
                  (utils/set-env "PACKET_PROJECT_ID" (utils/get-env "PROJECT_ID"))

                  (p/let  [install? (utils/prompts-delete cmd (str "Continue with the following config:" config))]
                    (when install?
                      (info "Installing k8 platform")
                      (command/run-command
                       #_(goog.string.format "./dependencies/kubeone-linux install <(echo \"%s\") -b %s" config service)
                       "kubeone" ["install" config])
                      (info "completed k8 install, check logs for errors"))))))
    (.. program
        (command "reset <config>")
        (option "-s --sshUsername [user]")
        (option "-k --sshprivKeyFile [key]")
        (action (fn [config cmd]
                  (p/let [_ (utils/set-env "PACKET_AUTH_TOKEN" (utils/get-env "APIKEY"))
                          _ (utils/set-env "PACKET_PROJECT_ID" (utils/get-env "PROJECT_ID"))
                          ;config  (kubeone/create-kubeone-config "packet" service cluster-network service-subnet )
                          reset? (utils/prompts-delete cmd (str "Reset the following config:" config))]
                    (when reset?
                      (info "Deleting k8 platform")
                      (p/let [result (command/run-command
                                  ;;(goog.string.format "./dependencies/kubeone-linux reset <(echo \"%s\")" config service)
                                      "kubeone" ["reset" config])]
                        (info "Completed k8 reset, check logs for errors")))))))

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
                                        (help #(clojure.string/replace % #"dci-platform-k8" "platform-k8")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (utils/kubeone-exists?)
  (command-handler))
