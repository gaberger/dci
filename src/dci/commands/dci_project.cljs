(ns dci.commands.dci-project
  (:require [commander]
            [util]
            [prompts]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [cljs.pprint :as pprint]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(def module-version "0.0.4")

(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "Project Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]

    (.. program
        (command "change <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (p/let [organization-id (utils/get-env "ORGANIZATION_ID")
                          project-id (api/get-projectid-prefix (keyword (.-provider program)) organization-id project-id)]
                    (when (some? project-id)
                      (p/let [project-name (api/get-project-name (keyword (.-provider program)) project-id)]
                        (utils/update-project-id project-id project-name)
                        (println "Switching to Project:" project-name "ID:" project-id)))))))

    (.. program
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
                    (api/print-projects (keyword (.-provider program)) organization-id)))))

    (.. program
        (command "create <project-name>")
        (option "-B --backend-transfer" "Enable Backend Transfer")
        (action (fn [project-name cmd]
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")
                        options (if (.-backendTransfer cmd)
                                  {:backend_transfer_enabled true}
                                  {:backend_transfer_enabled false})]
                    (api/create-project (keyword (.-provider program)) organization-id project-name options)))))

    (.. program
        (command "delete <project-id>")
        (option "-F --force" "Force Delete")
        (action (fn [project-id cmd]
                  (when (.-debug program) (utils/set-debug!))
                  (p/let [organization-id (utils/get-env "ORGANIZATION_ID")
                          project-id' (api/get-projectid-prefix (keyword (.-provider program)) organization-id project-id)]
                    (if (some? project-id')
                      (if (.-force cmd)
                        (api/delete-project (keyword (.-provider program)) project-id')
                        (p/let [delete? (utils/prompts-delete cmd (str "Delete Project: " project-id'))]
                          (when delete?
                            (api/delete-project (keyword (.-provider program)) project-id'))))
                      (println "Project:" project-id "Doesn't exist"))))))

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
                                        (help #(clojure.string/replace % #"dci-project" "project")))
      :else            (swap! app-state assoc :output :table))))

(defn main! []
  (command-handler))


