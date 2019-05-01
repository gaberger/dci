(ns lib.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [xhr2]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :as pprint]
           ; [xmlhttprequest :refer [XMLHttpRequest]]
            [server.dci-model :refer [IServer]]
            [dci.state :refer [app-state]]
            [utils.core :as utils]
            ))

;(set! js/XMLHttpRequest XMLHttpRequest) ;; weird status response
(set! js/XMLHttpRequest xhr2)

(def add-authentication-header
  {:name  ::add-authentication-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "X-Auth-Token"] (utils/get-env "APIKEY")))})


(defn bootstrap-packet-cljs []
  (martian-http/bootstrap "https://api.packet.net"
                          [
                           {:route-name :get-plans
                            :path-parts ["/plans"]
                            :summary    "Get plans listing"
                            :method     :get
                            }
                           {:route-name :get-organizations
                            :path-parts ["/organizations"]
                            :summary    "Get Organization listing"
                            :method     :get
                            }
                           {:route-name  :get-projects
                            :path-parts  ["/organizations/" :id "/projects"]
                            :summary     "Get projects listing"
                            :method      :get
                            :path-schema {:id s/Str}
                            }
                           {:route-name  :get-project
                            :path-parts  ["/projects/" :id]
                            :summary     "Get project"
                            :method      :get
                            :path-schema {:id s/Str}
                            }
                           {:route-name  :get-devices
                            :path-parts  ["/projects/" :id "/devices"]
                            :summary     "Get all devices of project"
                            :method      :get
                            :path-schema {:id s/Str}
                            :responses   {404 {:body s/Str}
                                          200 {:body s/Str}}}
                           {:route-name  :create-device
                            :path-parts  ["/projects/" :id "/devices"]
                            :path-schema {:id s/Str}
                            :summary     "Create device"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                            :body-schema {:hostname         s/Any
                                          :facility         [s/Any]
                                          :tags             [s/maybe s/Str]
                                          :plan             s/Any
                                          :operating_system s/Any}}
                           {
                            :route-name  :create-project
                            :path-parts  ["/organizations/" :id "/projects"]
                            :path-schema {:id s/Str}
                            :summary     "Create Project"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                            ;;:query-params {:exclude ["devices" "members" "memberships"
                            ;;                        "invitations" "max_devices" "ssh_keys"]
                            ;;               }
                            :body-schema {:name s/Any}}
                           {:route-name  :delete-project
                            :path-parts  ["/projects/" :id]
                            :path-schema {:id s/Str}
                            :summary     "Delete Project"
                            :method      :delete
                            }
                           {:route-name  :get-device
                            :path-parts  ["/devices/" :id]
                            :summary     "Get device listing"
                            :method      :get
                            :path-schema {:id s/Str}
                            }
                           {:route-name  :delete-device
                            :path-parts  ["/devices/" :id]
                            :summary     "Delete device"
                            :method      :delete
                            :path-schema {:id s/Str}
                            }]
                          {:interceptors (concat
                                          [add-authentication-header]
                                          martian-http/default-interceptors
                                          )}))

(defn response-handler [response]
  (let [status           (:status response)
        body             (:body response)
        error            (if (string? (:error body)) (:error body) "")
        error-and-exit   (fn []
                           (do (js/console.log "Error: Request Failed" status error))
                           (.exit js/process))
        success-and-exit (fn []
                           (do (println "Success" status)))]
    (when (:debug @app-state)
      (do
        (println "Response" response)
        (pprint/pprint (js->clj body true))))

  (condp = status
    401 (error-and-exit)
    404 (error-and-exit)
    422 (error-and-exit)
    406 body
    200 {:status 200 :body body}
    201 {:status 201 :body body}
    204 {:status 204 :body body}
    (error-and-exit)
    )))

(defn write-request [k opts]
  (when (:debug @app-state) (println "write-request" k opts))
  (go  (let [m        (bootstrap-packet-cljs)
             id       (:id opts)
             opts     (dissoc opts :id)
             _        (when (:debug @app-state)
                        (println "Body" opts)
                        (println "DryRun" (martian/request-for m k {:id            id
                                                                    ::martian/body opts})))
             response (<! (martian/response-for m k {:id            id
                                                     ::martian/body opts}))]
                (response-handler response))))

(defn read-request
  ([k opts]
  (when (:debug @app-state) (println "read-request" k opts))
   (go
     (let [m          (bootstrap-packet-cljs)
           _        (when (:debug @app-state) (println (martian/request-for m k opts)))
           response (<! (martian/response-for m k opts))]
         (response-handler response))))
  ([k]
   (read-request k nil)))



;; Implementations

(defn- get-organizations []
  (when (:debug @app-state) (println "calling get-organizations" ))
  (read-request :get-organizations))

(defn- print-organizations []
  (go
    (let [result        (<! (get-organizations))
          organizations (-> result :body :organizations)
          coll          (into []
                              (reduce
                               (fn [acc m]
                                 (conj acc (select-keys m [:id :name :description :account_id :monthly_spend])))
                               []
                               organizations))]
      (when (empty? organizations)
        (do (println "No Organizations found for user")
            (.exit js/process 0)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn project-exist? [name]
  (when (:debug @app-state) (println "calling project-exists?" name))
  (go
    (let [org-id   (utils/get-env "ORGANIZATION_ID")
          result   (<! (read-request :get-projects {:id org-id}))
          projects (-> result :body :projects)
          names (into #{} (map :name projects))]
      (contains? names name))))

(defn- get-projects []
  (when (:debug @app-state) (println "calling get-projects" ))
  (let [org-id (utils/get-env "ORGANIZATION_ID")]
    (read-request :get-projects {:id org-id})))

(defn- print-projects []
  #_{:pre [ (assert (and (not (nil? (utils/get-env "ORGANIZATION_ID")))
                         (not (nil? (utils/get-env "APIKEY")))) "ORGANIZATION_ID, APIKEY not set")]}
  (when (:debug @app-state) (println "calling list-projects" ))
 (go
   (let [result (<! (get-projects))
         projects (-> result :body :projects)
         coll     (into []
                        (reduce
                         (fn [acc m]
                           (let [id           (:id m)
                                 name         (:name m)
                                 device-count (-> m :devices count)
                                 member-count (-> m :members count)]
                             (conj acc {:id           id
                                        :name         name
                                        :device-count device-count
                                        :member-count member-count})))
                         []
                         projects))]
    (when (empty? projects)
      (do (println "No Projects found for user")
          (.exit js/process)))
    (condp = (:output @app-state)
      :json  (utils/print-json coll)
      :edn   (utils/print-edn coll)
      :table (utils/print-table coll)
      (utils/print-table coll)))))

(defn create-project [name]
  (when (:debug @app-state)  (println "calling create-project" name))
  (let [org-id (utils/get-env "ORGANIZATION_ID")]
    (go
      (if (<! (project-exist? name))
        (println "Error: Project" name "already exists")
        (write-request :create-project {:id org-id :name name})))))

(defn delete-project [id]
  (when (:debug @app-state) (println "calling delete-project" id))
  (read-request :delete-project {:id id}))

(defn- get-project-name [id]
  (when (:debug @app-state)  (println "calling get-project-name" id))
   (read-request :get-project {:id id}))

;;TODO Check for duplicate host-name or use a generating uuid
(defn create-device [{:keys [id name facilities tags plan os] :as args}]
  (when (:debug @app-state)  (println "calling create-device" args))
  (write-request :create-device {:id id
                                 :hostname name
                                 :facility facilities
                                 :tags [tags]
                                 :plan plan
                                 :operating_system os}))





(defn- list-plans []
  (when (:debug @app-state) (println "calling list-plans" ))
    (go
      (let [plans (:plans (<! (read-request :get-plans)))
            coll    (into []
                          (reduce
                           (fn [acc m]
                             (conj acc (select-keys m [:id :name :slug :description ])))
                           []
                           plans))]
        (condp = (:output @app-state)
          :json (utils/print-json coll)
          :edn   (utils/print-edn coll)
          :table (utils/print-table coll)
          )
        coll)))
    


(defn- get-device [id]
  (when (:debug @app-state) (println "calling get-device" ))
    (go
      (let [device (<! (read-request :get-device {:id id}))]
        (condp = (:output @app-state)
          :json (utils/print-json device)
          :edn   (utils/print-edn device)
          :table (utils/print-table device)
          )
        device)))

(defn- delete-device [id]
  (when (:debug @app-state) (println "calling delete-device" ))
  (go
    (let [device (<! (read-request :delete-device {:id id}))]
      device)))

(defn- list-devices
  "Parameters passed in vector argument [id options]"
  [{:keys [id options] :as args}]
  (when (:debug @app-state) (println "calling list-devices" args))
 (go
   (let [devices (:devices (<! (read-request :get-devices {:id id})))
         coll    (into []
                       (reduce
                        (fn [acc device]
                          (let [id               (:id device)
                                facility         (-> device :facility :name)
                                operating-system (-> device :operating_system :name)
                                hostname         (:hostname device)
                                tags             (if-not (str/blank? (first (:tags device)))
                                                   (into #{} (mapv #(str/trim %)(:tags device)))
                                                   [])
                                addresses        (into #{} (mapv #(:address %) (:ip_addresses device)))
                                network          (:network device)
                                root-password    (:root_password device)
                                state            (:state device)]
                            (conj acc {:id               id
                                       :facility         facility
                                       :operating-system operating-system
                                       :hostname         hostname
                                       :addresses        addresses
                                       :root-password    root-password
                                       :state            state
                                       :tags             tags})))
                        []
                        devices))
         coll    (if-not (empty? (:filter (:options options)))
                   (utils/filter-pred coll  (:filter (:options options)))
                   coll)]
     (when (empty? devices)
       (do (println "No Devices found for " id)
           (.exit js/process)))

     (condp = (:output @app-state)
       :json  (utils/print-json coll)
       :edn   (utils/print-edn coll)
       :table (utils/print-table coll)
       (utils/print-table coll)))))


(defn create-service [args]
  nil)

(defn- strip-device-item [i]
  (let [device        (str (:href i))
        device-id     (last (re-find #"/\w+/(\S+)" device))]
    device-id))



(deftype PacketServer []
  IServer
  (print-organizations [this] (print-organizations))
  (get-organizations [this] (get-organizations))

  (print-projects [this] (print-projects))
  (get-projects [this] (get-projects))
  (create-project [this args] (create-project (first args)))
  (delete-project [this args] (delete-project (first args)))
  (get-project-name [this args] (get-project-name (first args)))

  (list-plans [this] (list-plans))
  (get-server [this id] (get-device id))
  (list-servers [this args] (list-devices (first args)))
  (create-server [this args] (create-device  args))
  (create-service [this args] (create-service args))
  (delete-server  [this id] (delete-device id))
  (start-server  [this device-id] (println "start-server" device-id))
  (stop-server  [this device-id] (println "stop-server" name)))



;;Request URL:https://api.packet.net/organizations/60bdbe9f-10c2-4ac9-bc75-6fd635f52daa/projects?include=transfers&exclude=devices,members,memberships,invitations,max_devices,ssh_keys&token=mYsmWg6TfvXoQ436ptYU8T1Lip92M8GE
;;https://api.packet.net/organizations/60bdbe9f-10c2-4ac9-bc75-6fd635f52daa/projects?include=transfers&exclude=devices,members,memberships,invitations,max_devices,ssh_keys&token=mYsmWg6TfvXoQ436ptYU8T1Lip92M8GE

