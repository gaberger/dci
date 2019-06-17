(ns dci.components.kubeone
  (:require  [ip-utils :as ip]
             [dci.utils.core :as utils]
             [dci.drivers.interfaces :as api]
             [dci.drivers.packet]
             [taoensso.timbre :as timbre
              :refer-macros [log  trace  debug  info  warn  error  fatal  report
                             logf tracef debugf infof warnf errorf fatalf reportf
                             spy get-env]]
             [kitchen-async.promise :as p]
             [kitchen-async.promise.from-channel]))

(defn create-kubeone-config [provider cluster-name cluster-network service-subnet & options]
  (p/let [{:keys [ssh-user ssh-keyfile]} options
          ssh-user (or ssh-user  "root")
          ssh-keyfile (or ssh-keyfile (str (utils/get-home-dir) "/.ssh/id_rsa"))
          organization-id (utils/get-env "ORGANIZATION_ID")
          project-id (api/get-project-id (keyword provider) organization-id cluster-name)]
    (if (some? project-id)
      (p/let [devices  (api/get-devices-project (keyword provider) project-id)
              ssh-keys (api/get-ssh-keys (keyword provider))
              cp-devices (filterv (fn [m] (let [tag-set (into #{} (:tags m))]
                                            (contains? tag-set "control-plane"))) devices)
              create-hostdata (fn [devices] (into []
                                                 (reduce (fn [acc device]
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
                                                                                             (not (.isReserved ip x)))) addresses))]
                                                             (conj acc {:publicAddress     public-address
                                                                        :privateAddress    private-address
                                                                        :sshUsername       ssh-user
                                                                        :sshPrivateKeyFile ssh-keyfile}))) [] devices)))
              header {:apiVersion        "kubeone.io/v1alpha1"
                      :kind              "KubeOneCluster"
                      :name              cluster-name
                      :versions          {:kubernetes "1.14.1"}
                      :cloudProvider     {:name     "packet"
                                          :external true}
                      :clusterNetwork    {:podSubnet     cluster-network
                                          :serviceSubnet service-subnet}
                      :machineController {:deploy true}}
              hosts (create-hostdata cp-devices)
              ;;TODO Get facilities from project-id so we put workers where we have backend-transfer available
              ;;TODO Validate and get available OS and machine types.
              workers [{:name                   cluster-name
                        :replicas     3
                        :providerSpec {:labels              {:myLabel cluster-name}
                                       :cloudProviderSpec   {:instanceType "t1.small.x86"
                                                             :projectID project-id
                                                             :facilities ["ewr1"] }
                                      ; :sshPublicKeys       ssh-pub-keys
                                       :operatingSystem     "ubuntu"
                                       :operatingSystemSpec {:distUpgradeOnboot false}
                                       :SSHPublicKeys (if (some? ssh-keys) ssh-keys []) }


                        }]
              workers (if (get-in header [:machineController :deploy])
                         workers
                         [])
              apiendpoints  (mapv (fn [addr] {:host
                                              (:publicAddress addr)
                                              :port 6443}) hosts)
              config (-> header
                         (assoc :hosts hosts)
                         (assoc :workers workers)
                         #_(assoc :apiEndpoint apiendpoints))]
        (utils/write-yaml (clj->js config)))
      (error "Something Went wrong"))))


(def exports #js {})

;#kubeone wants to build the API Endpoint to the public addresss if an apiendpoint doesn't exist. Thie problem is that the private address are routeable but not contiguous so how do you represent the VIP address?

;# Setup IPVS
;#install ipvsadm
;#apt install ipvsadm
;# Create VIP
;#ipvsadm -A -t 1.2.3.4:80 -s rr
;#Add Nodes
;#ipvsadm -a -t 1.2.3.4:80 -r 172.17.0.3 -m
