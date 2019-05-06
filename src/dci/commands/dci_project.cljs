(ns dci.commands.dci-project
  (:require [commander]
            [util]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [cljs.pprint :as pprint]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (description "Project Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]

    (.. program
        (command "change <project-id>")
        (action (fn [project-id]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (p/let [result (api/get-project (keyword (.-provider program)) project-id)
                          name (-> result :body :name)]
                    (utils/update-project-id project-id name)
                    (println "Switching to Project" name)))))

    (.. program
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
                    (api/print-projects (keyword (.-provider program)) organization-id)))))

    (.. program
        (command "create <project-name>")
        (action (fn [project-name]
                  (println :DEBUG project-name)
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
                    (api/create-project (keyword (.-provider program)) organization-id {:name project-name})))))

    (.. program
        (command "delete <project-id>")
        (action (fn [projectId]
                  (when (.-debug program) (utils/set-debug!))
                  (api/delete-project
                   (keyword (.-provider program)) projectId))))
    (.. program
        (command "*")
        (action (fn []
                  (.help program #(clojure.string/replace % #"dci-organization" "organization")))))

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
              (help #(clojure.string/replace % #"dci-project" "project"))))))

(defn main! []
  (command-handler))


