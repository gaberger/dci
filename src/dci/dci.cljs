(ns dci.dci
  (:require [commander]
            [utils.core :as utils]
            [prompts]
            [cljs.nodejs.shell :as sh :include-macros true]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs-uuid-utils.core :as uuid]
            [clojure.set :as set]
            [dci.state :refer [app-state]]
            [server.dci-model :as model :refer [IServer list-organizations]]
            [lib.packet :as packet :refer [PacketServer]]
            [clojure.string :as str]))

(defmulti command-actions identity :default :default)
(defmethod command-actions :packet [& args]
  (condp = (second args)
    :list-organizations (list-organizations (PacketServer.))
    :default (println "Error: unknown command" (second args))))

(defn prompts-get-org [program]
  (p/let [orgs  (command-actions (keyword (.-provider program)) :list-organizations )
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
                 :validate (fn [x] (if (str/blank? x) "Enter API Key" true))
                 })
   (p/then (fn [apikey]
             (let [key (.-apikey apikey)]
               (utils/set-env "APIKEY" key)
               #_(swap! app-state assoc-in [:persist] {:apikey key}))))))

(defn prompts-save-state []
  (->
   (prompts #js {:type    "confirm"
                 :name    "save_state"
                 :message "Save APIKEY into statefile?"
                 :initial false
                 })
   (p/then (fn [x]
             (when (.-save_state x)
               (do
                 (utils/update-state)
                 (utils/save-state)
                 #_(js/console.log (.-env js/process))))))))

(defn command-handler []
    (.. commander
                    (version "0.0.1")
                    (command "organization <command>" "Organization operations")
                    (command "project <command>" "Project operations")
                    (command "server <command>" "Server operations")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    (description "Dell \"Bare Metal Cloud\" Command Interface"))
  #_(.parse  (.-argv js/process))
  )


 (defn main! []
   (let [program (command-handler)]
     (utils/update-environment)

     

     (condp (utils/get-env-keys)
       "APIKEY " (println "found key")
       (println "not found"))

     (when-not (utils/get-env "APIKEY")
       (p/do
         (prompts-get-key program)
         (prompts-get-org program)
         (prompts-save-state)
         (.parse program (.-argv js/process)))
     (when-not (utils/get-env "ORGANIZATION_ID")
       (p/do
         (prompts-get-org program)
         (.parse program (.-argv js/process))))

     )))

