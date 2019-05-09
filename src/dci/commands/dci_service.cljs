(ns dci.commands.dci-service
  (:require [commander]
            [util]
            [cljs.pprint :as pprint]
            [clojure.string :as str]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.utils.core :as utils]
            [dci.state :refer [app-state]]))

(enable-console-print!)

(defn command-handler []
  (let [program (.. commander
                    (version "0.0.1")
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
        (command "create <service-file>")
        (action (fn [service-file]
                  (when (.-debug program) (swap! app-state assoc :debug true))
                  (let [service-spec (utils/read-service-file service-file)]
                    (doall
                     (map (fn [m]
                            (p/let [{:keys [organization-id project-name service count plan facilities operating_system]} m
                                    organization-id (name organization-id)
                                    batch-create (fn [count project-id program]
                                                   (dotimes [x count]
                                                     (api/create-device
                                                      (keyword (.-provider program))
                                                      project-id {:plan             (name plan)
                                                                  :hostname         (str (name service) "-" x)
                                                                  :operating_system (name operating_system)
                                                                  :tags             (name service)
                                                                  :facility         (mapv name facilities)})))]
                              (go
                                (if-some [project-id
                                          (<! (api/create-project (keyword (.-provider program)) organization-id project-name))]
                                  (batch-create count project-id program)
                                  (let [project-id
                                        (<! (api/get-project-id (keyword (.-provider program)) organization-id project-name))]
                                    (batch-create count project-id program))))))
                          service-spec))))))

    (.. program
        (command "*")
        (action (fn []
                  (.help program #(clojure.string/replace % #"dci-service" "service")))))

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
