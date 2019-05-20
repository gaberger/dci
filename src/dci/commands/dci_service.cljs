(ns dci.commands.dci-service
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

(defn service-apply [provider project-id project-name node-spec]
  (p/let [devices (api/get-devices-project provider project-id)
          provisioning-devices (count (filterv #(= (:state %) "provisioning") devices))
          ]
    (if (> provisioning-devices 0)
      (error "Can't do apply while devices are provisioning.. Please try again")
      (mapv (fn [node-set]
              (let [{:keys [replicas plan facilities tags operating_system]} node-set
                    f-devices      (filterv (fn [m] (let [tag-set (into #{} (:tags m))]
                                                      (contains? tag-set (first tags)))) devices)
                    device-count                                             (count f-devices)]
                (cond
                  (< device-count replicas ) (dotimes [i (- replicas device-count)]
                                               (api/create-device
                                                provider
                                                project-id {:plan             plan
                                        ; :hostname         (str/join "-" [project-name (first tags) x])
                                                            :operating_system operating_system
                                                            :tags             tags
                                                            :facility         facilities}))
                  (> device-count replicas ) (let [prune-devices (->>
                                                                  (take (- device-count replicas) f-devices)
                                                                  (mapv :id))]
                                               (mapv #(api/delete-device provider %) prune-devices))
                  (= device-count replicas ) (info "Nothing to do for " project-name tags)
                  :else                      (error "Something went wrong" {:device-count device-count :replicas replicas})))) node-spec)
      )))
(defn command-handler []
  (let [program (.. commander
                    (version "0.0.4")
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
    ;TODO Change to batch
    (.. program
        (command "deploy <service-file>")
        (action (fn [service-file]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (p/try
                    (p/let [service-spec (utils/read-service-file service-file)
                            {:keys [organization_id project_name node_spec]} service-spec
                            project-id (api/get-project-id (keyword (.-provider program)) organization_id project_name)]
                          (if (some? project-id)
                            (service-apply :packet project-id project_name node_spec )
                            (error "Project doesn't exit")))
                    (p/catch js/Error e
                      (println "ERROR:" (js->clj e)))))))

    (.. program
        (command "install <command>" "This is an install"))

    (.on program "command:*" (fn [e]
                               (when-not
                                   (contains?
                                    (into #{}
                                          (keys (js->clj (.-_execs program))))
                                    (first e))
                                 (.help program))))

    (.parse program (.-argv js/process))
    (cond
      (.-json program) (swap! app-state assoc :output :json)
      (.-edn program)  (swap! app-state assoc :output :edn)
      :else            (swap! app-state assoc :output :table))

    (when (.-debug program) (do
                              (swap! app-state assoc :debug true)
                              (js/console.log program)
                              (pprint/pprint @app-state)))

  (cond (= (.-args.length program) 0)
        (.. program
            (help #(clojure.string/replace % #"dci-service" "service"))))))

(defn main! []
  (command-handler))
