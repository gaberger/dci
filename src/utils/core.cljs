(ns utils.core
  (:require [cljs-node-io.core :as io :refer [slurp spit ]]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs.reader :refer [read-string]]
            [util]
            [clojure.pprint :as pprint]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [dci.state :refer [app-state]]))

(def state-file ".dci-state.edn")

(defn print-edn [obj]
  (pprint/pprint  obj))

(defn print-json [obj]
    (println (.stringify js/JSON (clj->js obj) nil " ")))

;TODO filter empty set
(defn print-table [obj]
  (let [convert (postwalk #(if(and (set? %) (string? (first %))) (str/join "," %)  %) obj)]
    (pprint/print-table convert)))

(defn state-exists []
  (fexists? state-file))

(defn- write-state-file [data]
  (spit state-file data))

(defn- read-state-file []
  (read-string (slurp state-file)))

(defn dump-object [obj]
  (println "###########")
  (println (.inspect util obj)))

(defn get-env [v]
  (aget (.-env js/process) v ))

(defn- set-env [k v]
  (aset (.-env js/process) k v))

(defn initialize-state []
  (let [state {:lastrun (js/Date.)
               :runtime {:project-id nil
                         :project-name nil
                         :apikey nil
                         :organization-id nil}}]
    (write-state-file state)))

(defn save-state []
  (let [run-state (:runtime @app-state)
        state {:lastrun (js/Date.)
               :runtime run-state}]
    (write-state-file state)))

(defn update-project-id [id name]
  (let [state (read-state-file)]
    (->
     state
     (assoc :lastrun (js/Date.))
     (assoc :runtime {:project-id id :project-name name})
     (write-state-file))))

(defn get-project-id-state []
  (let [state (read-state-file)]
    [(-> state :runtime :project-id) (-> state :runtime :project-name)]))

(defn projectid? []
  (let [state (read-state-file)
        runtime (:runtime state)]
    (if-not (nil? (:project-id runtime))
      true
      false)))

(defn set-debug! []
  (swap! app-state assoc :debug true))

(defn get-environment []
  (initialize-state)
  (when-let [apikey (get-env "APIKEY")]
        (swap! app-state assoc-in [:runtime] {:apikey apikey})
    #_(do (println "Error: Set APIKEY environmental variable")
        (.exit js/process 1)))
  (when-let [org-id (get-env "ORGANIZATION_ID")]
    (swap! app-state update-in [:runtime] assoc :organization-id org-id))
  (save-state))



(defn filter-pred [data filter]
  (into []
        (for [m data
              :let  [tags (:tags m)]
              :when (contains? tags filter)]
      m)))
