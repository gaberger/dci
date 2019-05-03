(ns dci.dci
  (:require [commander]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.utils.core :as utils]
            [prompts]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.set :as set]
            [dci.state :refer [app-state]]
            [clojure.string :as str]))

(defn prompts-get-org [program]
  (p/let [response  (api/get-organizations (keyword (.-provider program)) false)
          orgs (-> response :body :organizations)
          choices  (mapv #(-> %
                              (select-keys [:id :name])
                              (set/rename-keys {:id :value :name :title})) orgs)
          org-select (prompts #js {:type    "select"
                                   :name    "orgid"
                                   :message "Select Primary Organization"
                                   :choices (clj->js choices)
                                   :initial 1})]
    (p/then org-select (fn [choice]
                         (let [orgid (.-orgid choice)]
                           (utils/set-env "ORGANIZATION_ID" orgid))))))

(defn prompts-get-key [program]
  (->
   (prompts #js {:type     "text"
                 :name     "apikey"
                 :message  "Enter API Key"
                 :validate (fn [x] (if (str/blank? x) "Enter API Key" true))})
   (p/then (fn [apikey]
             (let [key (.-apikey apikey)]
               (utils/set-env "APIKEY" key)
               #_(swap! app-state assoc-in [:persist] {:apikey key}))))))

(defn prompts-save-state []
  (->
   (prompts #js {:type    "confirm"
                 :name    "save_state"
                 :message "Save APIKEY into statefile?"
                 :initial false})
   (p/then (fn [x]
             (when (.-save_state x)
               (do
                 (utils/update-state)
                 (utils/save-config)
                 #_(js/console.log (.-env js/process))))))))

(defn command-handler []
  (.. commander
      (version "0.0.1")
      (command "organization <command>" "Organization operations")
      (command "project <command>" "Project operations")
      (command "server <command>" "Server operations")
      (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
      (description "Dell \"Bare Metal Cloud\" Command Interface"))
  #_(.parse  (.-argv js/process)))

(defn main! []
  (utils/update-environment)
  (utils/initialize-state)
  (swap! app-state assoc :output :json)
  (let [program  (command-handler)
        env-keys (utils/get-env-keys)]
    (cond
      (every? env-keys #{"APIKEY" "ORGANIZATION_ID"}) (p/do
                                                        (.parse program (.-argv js/process)))

      (contains? env-keys "ORGANIZATION_ID") (p/do
                                               (prompts-get-key program)
                                               (prompts-save-state)
                                               (.parse program (.-argv js/process)))
      (contains? env-keys "APIKEY")          (p/do
                                               (prompts-get-org program)
                                               (prompts-save-state)
                                               (.parse program (.-argv js/process)))
      :else                                  (p/do
                                               (prompts-get-key program)
                                               (prompts-get-org program)
                                               (prompts-save-state)
                                               (.parse program (.-argv js/process))))))

