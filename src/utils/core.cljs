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
  (println "Write-state-file" data)
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
               :runtime {:project-id nil}}]
    (write-state-file state)))

(defn update-project-id [id]
  (let [state (read-state-file)]
    (->
     state
     (assoc-in [:runtime :project-id] id)
     (assoc :lastrun (js/Date.))
     (write-state-file))))

(defn get-project-id []
  (let [state (read-state-file)]
    (-> state :runtime :project-id)))

(defn projectid? []
  (let [state (read-state-file)
        runtime (:runtime state)]
    (if-not (nil? (:project-id runtime))
      true
      false)))

(defn get-api-token []
  (if (nil? (get-env "APIKEY"))
    (do (println "Error: Set APIKEY environmental variable")
        (.exit js/process 1))
    (swap! app-state assoc :apikey  (get-env "APIKEY"))))
