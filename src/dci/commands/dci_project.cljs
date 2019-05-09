(ns dci.commands.dci-project
  (:require [commander]
            [util]
            [prompts]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [cljs.pprint :as pprint]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defn prompts-delete-project [program]
  (->
   (prompts #js {:type     "confirm"
                 :name     "delete_project"
                 :message  "Delete Project?"
                 :validate (fn [x] (if (str/blank? x) "Delete Project?" true))})
   (p/then (fn [x]
             (if (.-delete_project x)
               true
               false)))))

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
                  (p/let [organization-id (utils/get-env "ORGANIZATION_ID")
                          result (api/get-projects (keyword (.-provider program)) organization-id)
                          projects (-> result :body :projects)
                          project-ids (into #{} (mapv :id projects))
                          project-selector  (utils/prefix-match project-id project-ids)
                          project-m (fn [project-id] (filterv #(= (:id %) project-id) projects))]
                    (if (some? project-selector)
                      (let [project-name (:name (first (project-m project-selector)))
                            _ (println "PROJECT_NAME"  project-name project-selector)]
                        (utils/update-project-id project-selector project-name)
                        (println "Switching to Project" project-name))
                      (if (contains? project-ids project-id)
                        (let [project-name (:name (first (project-m project-id)))]
                          (utils/update-project-id project-id project-name)
                          (println "Switching to Project" project-name))
                          (println "Error: Project " project-id "doesn't exist")))))))

    (.. program
        (command "list")
        (action (fn []
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
                    (api/print-projects (keyword (.-provider program)) organization-id)))))

    (.. program
        (command "create <project-name>")
        (action (fn [project-name]
                  (when (.-debug program) (utils/set-debug!))
                  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
                    (api/create-project (keyword (.-provider program)) organization-id project-name)))))

    (.. program
        (command "delete <project-id>")
        (option "-F --force" "Force Delete")
        (action (fn [project-id force cmd]
                  (if-not force
                    (p/let [delete? (prompts-delete-project cmd)]
                      (when delete?
                        (when (.-debug program) (utils/set-debug!))
                        (p/let [organization-id (utils/get-env "ORGANIZATION_ID")
                                result (api/get-projects (keyword (.-provider program)) organization-id)
                                projects (-> result :body :projects)
                                project-ids (into #{} (mapv :id projects))
                                project-selector  (utils/prefix-match project-id project-ids)]
                          (if (some? project-selector)
                            (api/delete-project (keyword (.-provider program)) project-selector)
                            (if (contains? project-ids project-id)
                              (api/delete-project (keyword (.-provider program)) project-id)
                              (println "Error: Project " project-id "doesn't exist"))))))))))

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


