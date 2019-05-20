(ns dci.utils.core
  (:require [cljs-node-io.core :as io :refer [slurp spit]]
            [cljs-node-io.fs :refer [fexists?]]
            [cljs.reader :refer [read-string]]
            [util]
            [prompts]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [os]
            [js-yaml :as yaml]
            [path]
            [clj-fuzzy :as fuz]
            [goog.object :as obj]
            [clojure.pprint :as pprint]
            [clojure.walk :refer [postwalk]]
            [clojure.string :as str]
            [dci.state :refer [app-state]]))

(def state-file (str/join "/" [(.homedir os) ".dci-state.edn"]))
(def config-file (str/join "/" [(.homedir os) "dci-config.edn"]))


(defn get-home-dir [] (.homedir os))

(defn print-edn [obj]
  (pprint/pprint  obj))

(defn print-json [obj]
  (println (.stringify js/JSON (clj->js obj) nil " ")))

;TODO filter empty set
(defn print-table [obj]
  (let [convert-set (postwalk #(if (and (set? %) (string? (first %))) (str/join "," %)  %) obj)
        convert-keys (postwalk #(if (keyword? %) (name %) %) convert-set)]
    (pprint/print-table convert-keys)))

(defn read-yaml [string]
 (try
   [:ok (js->clj (yaml/load string) :keywordize-keys true)]
   (catch js/Object e
     [:error (js->clj e)])))

(defn write-yaml [js]
  (try
    (yaml/safeDump js)
    (catch js/Object e
      [:error (js->clj e)])))


(defn log-error [& msg]
  #_(do (binding [*print-fn* *print-err-fn*]))
  (js/console.error (str/join " " msg)))

(defn state-exists []
  (fexists? state-file))

(defn config-exists? []
  (fexists? config-file))

(defn- write-state-file [data]
  (try
    (spit state-file data)
    (catch js/Object e
        (js->clj e))))

(defn- write-config-file [data]
  (try
    (spit config-file data)
    (catch js/Object e
       (js->clj e))))

(defn read-state-file []
  (try
    (read-string (slurp state-file))
    (catch js/Object e
       (js->clj e))))

(defn read-config-file []
  (try
    (read-string (slurp config-file))
    (catch js/Object e
      (js->clj e))))

(defn read-service-file [file]
  (try
    (fnext (read-yaml (slurp file)))
    (catch js/Object e
       (js->clj e))))

(defn dump-object [obj]
  (println "###########")
  (println (.inspect util obj)))

(defn get-env [v]
  (aget (.-env js/process) v))

(defn- set-env [k v]
  (aset (.-env js/process) k v))

(defn get-env-keys []
  (into #{} (-> (obj/getKeys (.-env js/process)) (js->clj))))

(defn initialize-state []
  (if-not (state-exists)
    (let [state {:lastrun (js/Date.)
                 :runtime {:project-id nil
                           :project-name nil}}]
      (write-state-file state))))

(defn save-state []
  (let [runtime (:runtime @app-state)
        state {:lastrun (js/Date.)
               :runtime runtime}]
    (write-state-file state)))

(defn save-config []
  (let [config (:persist @app-state)]
    (write-config-file config)))

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

(defn update-state []
  (when-let [apikey (get-env "APIKEY")]
    (swap! app-state assoc-in [:persist] {:apikey apikey}))
  (when-let [org-id (get-env "ORGANIZATION_ID")]
    (swap! app-state update-in [:persist] assoc :organization-id org-id)))

(defn update-environment []
  (if (config-exists?)
    (do
      (when-let [apikey (some-> (read-config-file) :apikey)]
        (set-env "APIKEY" apikey))
      (when-let [org-id (some-> (read-config-file) :organization-id)]
        (set-env "ORGANIZATION_ID" org-id))))
  (if (state-exists)
    (do
      (when-let [project-id (some-> (read-state-file) :runtime :project-id)]
        (set-env "PROJECT_ID" project-id)))))

(defn update-project-id [id name]
  (let [state (read-state-file)]
    (->
     state
     (assoc :lastrun (js/Date.))
     (assoc :runtime {:project-id id :project-name name})
     (write-state-file))
    (update-environment)))

(defn filter-pred [data filter]
  (when (:debug @app-state) (println "Using filter" filter))
  (into []
        (for [m data
              :let  [tags (:tags m)]
              :when (contains? tags filter)]
          m)))

(defn prefix-match [match coll]
  (when (:debug @app-state) (println "prefix-match" match))
  (let [matches (for [x     coll
                      :when (str/starts-with? x match)]
                  x)]
    (if (or (> (count matches) 1)
            (empty? matches))
      nil
      (do (when (:debug @app-state) (println "match" (first matches)))
          (first matches)))))

(defn selector [input coll]
  (let [str->vec (mapv str input)
        collv    (into [] coll)
        accum    (atom [])]
    (when (:debug @app-state) (println "selector" str->vec collv))
    (conj accum (first str->vec))
    (reduce
     (fn [acc x]
       (let [match-string (str/join "" @accum)]
         (when (:debug @app-state (println "match-string" match-string)))
         (when (some? (prefix-match match-string collv))
             (reduced :FOO #_(prefix-match match-string collv)))
       (swap! accum conj x))
       )
     []
     str->vec)))


(defn prompts-delete [program msg]
    (->
     (prompts #js {:type     "confirm"
                   :name     "delete"
                   :message  msg
                   :validate (fn [x] (if (str/blank? x) msg true))})
     (p/then (fn [x]
               (if (.-delete x)
                 true
                 false)))))



(def exports #js {})
