(ns utils.core
  (:require [cljs-node-io.core :as io :refer [slurp spit ]]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs.reader :refer [read-string]]
            [util]
            [dci.state :refer [app-state]]))

(def state-file ".dci-state.edn")

(defn print-json [obj]
  (println (.stringify js/JSON (clj->js obj) nil " ")))

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
                         :project-name nil}}]
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

(defn get-api-token []
  (if (nil? (get-env "APIKEY"))
    (do (println "Error: Set APIKEY environmental variable")
        (.exit js/process 1))
    (swap! app-state assoc :apikey  (get-env "APIKEY"))))
