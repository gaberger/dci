(ns dci.commands.dci-server
  (:require [commander]
            [util]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
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
                                     (string? project-id) project-id
                                     :else (.help cmd (fn [t] t)))
                        tag (if (.-tag cmd) {:filter (.-tag cmd)} {:filter []})]
                    (when (= (:output @app-state) :table)
                      (p/let [project-name  (api/get-project-name (keyword (.-provider program)) project-id)]
                        (println "Using Project:" project-name "\nID:" project-id)))
                    (api/print-devices-project (keyword (.-provider program))  project-id {:tag tag})))))

    (.. program
        (command "create <hostname>")
        (arguments "[project-id]")
        (option "-L --plan <plan>" "Select Plan")
        (option "-F --facilities <facilities>" "Comma seperated list of preferred facilities")
        (option "-O --os <os>" "Select Operating System")
        (option "-T --tags <tags>" "Comma seperated list of tags to apply to metadata")
        (action (fn [hostname project-id cmd]
                  (let [project-id (cond
                                     (some? (utils/get-env "PROJECT_ID")) (utils/get-env "PROJECT_ID")
                                     (string? project-id) project-id
                                     :else (.help cmd (fn [t] t)))
                        hostname'      (str hostname)
                        plan'          (or (.-plan cmd) "baremetal_0")
                        tags'          (or (.-tags cmd)  nil)
                        facilities'    (or (.-facilities cmd) ["ewr1"])
                        os'            (or (.-os cmd) "ubuntu_16_04")
                        args           {:hostname         hostname'
                                        :plan             plan'
                                        :facility         facilities'
                                        :tags             tags'
                                        :operating_system os'}]
                    (api/create-device (keyword (.-provider program)) project-id args)))))

    (.. program
        (command "delete <device-id>")
        (action (fn [device-id options]
                  (api/delete-device (keyword (.-provider program)) device-id))))
    (.. program
        (command "*")
        (action (fn []
                  (.help program #(clojure.string/replace % #"dci-server" "server")))))

    (.parse program (.-argv js/process))

    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      :else            (swap! app-state assoc :output :table))

    (when (.-debug program) (do
                              (swap! app-state assoc :debug true)
                              (pprint/pprint @app-state)))

    (cond (= (.-args.length program) 0)
          (.. program
              (help #(clojure.string/replace % #"dci-server" "server"))))))

(defn main! []
  (command-handler))
