#_(ns dci.main
  (:require [commander]
            [utils.core :as utils]
            [child_process]
            [prompts]
            [path]
            [fs]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs-uuid-utils.core :as uuid]
            [clojure.set :as set]
            [dci.state :refer [app-state]]
            [server.dci-model :as model :refer [IServer list-organizations]]
            [lib.packet :as packet :refer [PacketServer]]
            [clojure.string :as str]))

;; (defmulti command-actions identity :default :default)
;; (defmethod command-actions :packet [& args]
;;   (condp = (second args)
;;     :list-organizations (list-organizations (PacketServer.))
;;     :default (println "Error: unknown command" (second args))))


;; (defn prompts-get-org []
;;   (p/let [orgs  (command-actions :packet :list-organizations )
;;           choices  (mapv #(-> %
;;                               (select-keys [:id :name])
;;                               (set/rename-keys {:id :value :name :title})) orgs)
;;           org-select (prompts #js {:type    "select"
;;                                    :name    "orgid"
;;                                    :message "Select Primary Organization"
;;                                    :choices (clj->js choices)
;;                                    :initial 1})]
;;     (p/then org-select (fn [choice]
;;                          (let [orgid (.-orgid choice)]
;;                            (utils/set-env "ORGANIZATION_ID" orgid))))))

;; (defn prompts-get-key []
;;   (->
;;    (prompts #js {:type     "text"
;;                  :name     "apikey"
;;                  :message  "Enter API Key"
;;                  :validate (fn [x] (if (str/blank? x) "Enter API Key" true))
;;                  })
;;    (p/then (fn [apikey]
;;              (let [key (.-apikey apikey)]
;;                (utils/set-env "APIKEY" key)
;;                #_(swap! app-state assoc-in [:persist] {:apikey key}))))))

;; (defn prompts-save-state []
;;   (->
;;    (prompts #js {:type    "confirm"
;;                  :name    "save_state"
;;                  :message "Save APIKEY into statefile?"
;;                  :initial false
;;                  })
;;    (p/then (fn [x]
;;              (when (.-save_state x)
;;                (do
;;                  (utils/update-environment)
;;                  (utils/save-state)
;;                  #_(js/console.log (.-env js/process))))))))



;; (defn main! []
;;   (let [switchboard   "switchboard"
;;         cwd           (. js/process (cwd))
;;         launch-dir    (. path (dirname (second (.-argv js/process))))
;;         resolved-path (. path (join launch-dir switchboard))
;;         js-path       (if (. fs (existsSync (str resolved-path ".js")))
;;                         (str resolved-path ".js") nil)
;;         args          (into [] (nnext (js->clj (.-argv js/process))))]

;;     (if-not (utils/get-env "APIKEY")
;;       (p/do
;;         (prompts-get-key)
;;         (prompts-get-org)
;;         (prompts-save-state)
;;         (. child_process (spawnSync js-path  (clj->js args) #js {:shell false
;;                                                                  :stdio   "inherit"})))
;;       (p/do
;;         (prompts-get-org)
;;         (. child_process (spawnSync js-path  (clj->js args) #js {:shell false
;;                                                                  :stdio   "inherit"})))
;;       )))

          



