(ns dci.commands.dci-server
  (:require [commander]
            [util]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(def module-version "0.0.4")

(defn command-handler []
  (p/let [program (.. commander
                      (version module-version)
                      (description "Server Module")
                      (option "-D --debug" "Debug")
                      (option "-J --json" "Output to JSON")
                      (option "-E --edn" "Output to EDN")
                      (option "-P --provider <provider>" "Provider"  #"(?i)(packet|vultr)$" "packet"))]

    (.. program
        (command "list")
        (arguments "[project-id]")
        (option "-T --tag <tag>" "Filter list by tag")
        (action (fn [project-id cmd]
                  (let [project-id (cond
                                     (some? (utils/get-env "PROJECT_ID")) (utils/get-env "PROJECT_ID")
                                     (string? project-id)                 project-id
                                     :else                                (.help cmd (fn [t] t)))
                        tag        (if-some [filter (.-tag cmd)] {:filter filter} nil)]
                    (api/print-devices-project :packet project-id tag)
                    #_(p/let [project-name  (api/get-project-name (keyword (.-provider program)) project-id)]
                        (utils/log-error "Using Project:" project-name "\nID:" project-id)))
                  #_(api/print2-devices-project (keyword (.-provider program))  project-id filter))))

    (.. program
        (command "create <hostname>")
        (arguments "[project-id]")
        (option "-L --plan <plan>" "Select Plan")
        (option "-F --facilities <facilities>" "Comma seperated list of preferred facilities")
        (option "-O --os <os>" "Select Operating System")
        (option "-T --tags <tags>" "Comma seperated list of tags to apply to metadata")
        (action (fn [hostname project-id cmd]
                  (p/try
                    (p/let [project-id  (cond
                                          (some? (utils/get-env "PROJECT_ID")) (utils/get-env "PROJECT_ID")
                                          (string? project-id)                 project-id
                                          :else                                (.help cmd (fn [t] t)))
                            hostname'   (str hostname)
                            plan'       (or (.-plan cmd) "baremetal_0")
                            tags'       (or (.-tags cmd)  nil)
                            facilities' (or (.-facilities cmd) ["ewr1"])
                            os'         (or (.-os cmd) "ubuntu_16_04")
                            args        {:hostname         hostname'
                                         :plan             plan'
                                         :facility         facilities'
                                         :tags             tags'
                                         :operating_system os'}]
                      (api/create-device (keyword (.-provider program)) project-id args))
                    (p/catch js/Error e
                      (println e))))))

      ;(.. program
      ;  (command "gen-inventory <service-name>")
      ;  (action (fn [service-name cmd]
      ;            (go
      ;              (let [organization-id (utils/get-env "ORGANIZATION_ID")]
      ;                (api/gen-inventory (keyword (.-provider program)) organization-id service-name))))))


    (.. program
        (command "delete <device-id>")
        (option "-F --force" "Force Delete")
        (action (fn [device-id cmd]
                  (p/try
                    (p/let [project-id (utils/get-env "PROJECT_ID")
                            device-id' (api/get-deviceid-prefix (keyword (.-provider program)) project-id device-id)]
                      (when (some? device-id')
                        (if (.-force cmd)
                          (api/delete-device (keyword (.-provider program)) device-id')
                          (p/let [delete? (utils/prompts-delete cmd (str "Delete Device: " device-id'))]
                            (when delete?
                              (api/delete-device (keyword (.-provider program)) device-id'))))))
                    (p/catch js/Error e
                      (error "Something went wrong with delete " e))))))

    (.. program
        (command "events <device-id>")
        (action (fn [device-id cmd]
                  (p/try
                    (p/let [project-id (utils/get-env "PROJECT_ID")
                            device-id' (api/get-deviceid-prefix (keyword (.-provider program)) project-id device-id)]
                      (when (some? device-id')
                        (p/let [events (api/print-device-events (keyword (.-provider program)) device-id')]
                          (utils/print-json events))))
                    (p/catch js/Error e
                      (println e))))))


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
                                        (help #(clojure.string/replace % #"dci-server" "server")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (command-handler))
