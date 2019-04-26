(ns dci.main
  (:require [commander]
            [utils.core :as utils]
            [prompts]
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

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
                    (command "organization <command>" "Organization operations")
                    (command "project <command>" "Project operations")
                    (command "server <command>" "Server operations")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet")
                    (description "Dell \"Bare Metal Cloud\" Command Interface")
                    #_(parse (.-argv js/process)))]
    
    (if-not (utils/get-env "APIKEY")
      (p/let [api-p  (prompts #js {:type     "text"
                                   :name     "apikey"
                                   :message  "Enter API Key"
                                   :validate (fn [x] (if (str/blank? x) "Enter API Key" true))
                                   })]
                  (p/then api-p (fn [apikey]
                                  (let [key (.-apikey apikey)]
                                    (utils/set-env "APIKEY" key)
                                    (swap! app-state assoc-in [:runtime] {:apikey key})
                                    )))
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
                             (utils/set-env "ORGANIZATION_ID" orgid))))

        (.parse program (.-argv js/process))))
      ;;else
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
                               (utils/set-env "ORGANIZATION_ID" orgid))))

        (.parse program (.-argv js/process)))
      )))


(defn main! []
  (utils/get-environment)
  (command-handler))

