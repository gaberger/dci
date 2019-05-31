(ns dci.commands.dci-cluster
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(def module-version "0.0.4")


;;TODO call verify-userdata
(defn cluster-apply [provider project-id project-name node-spec]
  (p/let [devices (api/get-devices-project provider project-id)
          provisioning-devices (count (filterv #(= (:state %) "provisioning") devices))
          ]
    (if (> provisioning-devices 0)
      (error "Can't do apply while devices are provisioning.. Please try again")
      (mapv (fn [node-set]
              (let [{:keys [replicas plan facilities tags operating_system distribute userdata]} node-set
                    f-devices    (filterv (fn [m] (let [tag-set (into #{} (:tags m))]
                                                    (contains? tag-set (first tags)))) devices)
                    facility-s (atom [])
                    device-count (count f-devices)]
                (reset! facility-s facilities)
                (cond
                  (< device-count replicas ) (dotimes [i (- replicas device-count)]
                                               (api/create-device
                                                provider
                                                project-id {:plan             plan
                                        ;TODO should we compose our own name
                                                            ;;:hostname
                                                            :operating_system operating_system
                                                            :tags             tags
                                                            :userdata         userdata
                                                            :facility         (if distribute
                                                                                (do
                                                                                  (reset! facility-s (utils/rotate-left @facility-s))
                                                                                  @facility-s)
                                                                                @facility-s)}))
                  (> device-count replicas ) (let [prune-devices (->>
                                                                  (take (- device-count replicas) f-devices)
                                                                  (mapv :id))]
                                               (mapv #(api/delete-device provider %) prune-devices))
                  (= device-count replicas ) (info "Nothing to do for " project-name tags)
                  :else                      (error "Something went wrong" {:device-count device-count :replicas replicas})))) node-spec)
      )))
(defn command-handler []
  (let [program (.. commander
                    (version module-version)
                    (description "Service Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]


    ;;[{:project 1231
    ;;:service  :etcd
    ;;:count    3
    ;;:plan     :baremetal_0
    ;;:location [:ewr1]
    ;;:os       :ubuntu_16-04
    ;; }
    ;;]
    ;; TODO Change to batch and create project if it doesn't exisst

    (.. program
        (command "deploy <cluster-file>")
        (action (fn [cluster-file]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (p/try
                    (p/let [cluster-spec (utils/read-cluster-file cluster-file)
                            {:keys [organization_id project_name node_spec]} cluster-spec
                            project-id (api/get-project-id (keyword (.-provider program)) organization_id project_name)]
                          (if (some? project-id)
                            (cluster-apply :packet project-id project_name node_spec )
                            (error "Project doesn't exit")))
                    (p/catch js/Error e
                      (println "ERROR:" (js->clj e)))))))

    (utils/handle-command-default program) 

    (.parse program (.-argv js/process))
    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      :else            (swap! app-state assoc :output :table))

    (when (.-debug program) (swap! app-state assoc :debug true)
                              (js/console.log program)
                              (pprint/pprint @app-state))

    (cond (= (.-args.length program) 0)
          (.. program
              (help #(clojure.string/replace % #"dci-cluster" "cluster"))))))

(defn main! []
  (command-handler))
