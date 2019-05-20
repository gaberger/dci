(ns dci.commands.dci-service-install
  (:require [commander]
            [util]
            [ip-utils :as ip]
            [child_process :as proc]
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
                    (version "0.0.4")
                    (description "Service Install Module")
                    (option "-D --debug" "Debug")
                    (option "-J --json" "Output to JSON")
                    (option "-E --edn" "Output to EDN")
                    (option "-P --provider <provider>" "Provider"  #"(?i)(packet|softlayer)$" "packet"))]


    (.. program
        (command "kubeone <service> <cluster-network> <service-subnet>")
        (option "-s --sshUsername [user]")
        (option "-k --sshprivKeyFile [key]")

        (action (fn [service cluster-network service-subnet cmd]
                  #_(let [cp (proc/spawn "./dependencies/kubeone-linux" ["--help"] {})]
                    (.on (.-stdout cp) "data" (fn [data] (println (.toString data))))
                    (when (.-debug program) (swap! app-state assoc :debug true)))

                  (p/let [ssh-user (or (.-user cmd) "root")
                          ssh-keyfile (or (.-key cmd) (str (utils/get-home-dir) "/.ssh/id_rsa"))
                          organization-id (utils/get-env "ORGANIZATION_ID")
                          project-id (api/get-project-id (keyword (.-provider program)) organization-id service)
                          devices (api/get-devices-project (keyword (.-provider program)) project-id)
                          header {:apiVersion     "kubeone.io/v1alpha1"
                                  :kind           "KubeOneCluster"
                                  :name           service
                                  :versions       {:kubernetes "1.14.1"}
                                  :cloudProvider  {:name     "packet"
                                                   :external true}
                                  :clusterNetwork {:podSubnet     cluster-network
                                                   :serviceSubnet service-subnet}
                                  :machineController {:deploy true}}
                          body (into [] (reduce (fn [acc device]
                                                  (let [addresses       (mapv #(:address %) (:ip_addresses device))
                                                        private-address (first
                                                                         (filter (fn [x]
                                                                                   (and
                                                                                    (.isValidIpv4 ip x)
                                                                                    (.isPrivate ip x))) addresses))
                                                        public-address  (first
                                                                         (filter (fn [x]
                                                                                   (and
                                                                                    (.isValidIpv4 ip x)
                                                                                    (not(.isReserved ip x)))) addresses))]
                                                    (conj acc {:publicAddress     public-address
                                                               :privateAddress    private-address
                                                               :sshUsername       ssh-user
                                                               :sshPrivateKeyFile ssh-keyfile}))) [] devices))
                          workers (if (get-in header [:machineController :deploy])
                                    (let [workers {:workers []}]
                                      workers))]
                    (println (utils/write-yaml (clj->js (assoc-in header [:hosts] body))))
                    ))))


    (defn patch-deploy []
    ;  (comment """ sed "s/__admission_ca_cert__/$(shell cat examples/ca-cert.pem|base64 -w0)/g" 
		;     |sed "s/__admission_cert__/$(shell cat examples/admission-cert.pem|base64 -w0)/g" 
    ;  |sed "s/__admission_key__/$(shell cat examples/admission-key.pem|base64 -w0)/g" """))/ca_
      )



    (.on program "command:*" (fn [e]
                               (when-not
                                   (contains?
                                    (into #{}
                                          (keys (js->clj (.-_execs program))))
                                    (first e))
                                 (.help program))))

    #_(.. program
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

