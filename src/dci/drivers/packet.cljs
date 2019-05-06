(ns dci.drivers.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [xhr2]
            [xmlhttprequest :refer [XMLHttpRequest]]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :as pprint]
            [dci.state :refer [app-state]]
            [dci.drivers.interfaces :as api]
            [dci.utils.core :as utils]))

(set! js/XMLHttpRequest xhr2)
(set! js/XMLHttpRequest XMLHttpRequest) ;; weird status response


(def add-authentication-header
  {:name  ::add-authentication-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "X-Auth-Token"] (utils/get-env "APIKEY")))})

(defn bootstrap-packet-cljs []
  (martian-http/bootstrap "https://api.packet.net"
                          [{:route-name :get-plans
                            :path-parts ["/plans"]
                            :summary    "Get plans listing"
                            :method     :get}
                           {:route-name :get-organizations
                            :path-parts ["/organizations"]
                            :summary    "Get Organization listing"
                            :method     :get}
                           {:route-name  :get-projects
                            :path-parts  ["/organizations/" :organization-id "/projects"]
                            :summary     "Get projects listing"
                            :method      :get
                            :path-schema {:organization-id s/Str}}
                           {:route-name  :get-project
                            :path-parts  ["/projects/" :project-id]
                            :summary     "Get project"
                            :method      :get
                            :path-schema {:project-id s/Str}}
                           {:route-name  :get-devices-project
                            :path-parts  ["/projects/" :project-id "/devices"]
                            :summary     "Get all devices of project"
                            :method      :get
                            :path-schema {:project-id s/Str}}
                           {:route-name  :get-devices-organization
                            :path-parts  ["/organizations/" :organization-id "/devices"]
                            :summary     "Get all devices of organization"
                            :method      :get
                            :path-schema {:organization-id s/Str}}
                           {:route-name  :create-device
                            :path-parts  ["/projects/" :project-id "/devices"]
                            :path-schema {:project-id s/Any}
                            :summary     "Create device"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                            :body-schema {:hostname         s/Any
                                          :facility         [s/Any]
                                          :tags             [s/maybe s/Str]
                                          :plan             s/Any
                                          :operating_system s/Any}}
                           {:route-name  :create-project
                            :path-parts  ["/organizations/" :organization-id "/projects"]
                            :path-schema {:organization-id s/Str}
                            :summary     "Create Project"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                            ;;:query-params {:exclude ["devices" "members" "memberships"
                            ;;                        "invitations" "max_devices" "ssh_keys"]
                            ;;               }
                            :body-schema {:name s/Any}}
                           {:route-name  :delete-project
                            :path-parts  ["/projects/" :project-id]
                            :path-schema {:project-id s/Str}
                            :summary     "Delete Project"
                            :method      :delete}
                           {:route-name  :get-device
                            :path-parts  ["/devices/" :device-id]
                            :summary     "Get device listing"
                            :method      :get
                            :path-schema {:device-id s/Str}}
                           {:route-name  :delete-device
                            :path-parts  ["/devices/" :device-id]
                            :summary     "Delete device"
                            :method      :delete
                            :path-schema {:device-id s/Str}}]
                          {:interceptors (concat
                                          [add-authentication-header]
                                          martian-http/default-interceptors)}))

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
      (error-and-exit))))

(defn write-request [k path-m body]
  (when (:debug @app-state) (println "write-request" k path-m body))
  (go  (let [m        (bootstrap-packet-cljs)
             _        (when (:debug @app-state)
                        (do
                          (println "Path" path-m "Body" body)
                          (println "DryRun" (martian/request-for m k (merge
                                                                      path-m
                                                                      {::martian/body body})))))
             response (<! (martian/response-for m k (merge path-m
                                                           {::martian/body body})))]
         (response-handler response))))

(defn read-request
  ([k path-m body]
   (when (:debug @app-state) (println "read-request" k path-m body))
   (go
     (let [m        (bootstrap-packet-cljs)
           _        (when (:debug @app-state)
                      (println (martian/request-for m k (merge path-m
                                                               {::martian/body body}))))
           response (<! (martian/response-for m k (merge path-m
                                                         {::martian/body body})))]
       (response-handler response))))
  ([k path-m]
   (read-request k path-m nil))
  ([k]
   (read-request k nil nil)))


;; Implementations


(defn- get-organizations []
  (when (:debug @app-state) (println "calling get-organizations"))
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

(defn project-exist? [{:keys [organization-id name]}]
  (when (:debug @app-state) (println "calling project-exists?" name))
  (go
    (let [result   (<! (read-request :get-projects {:organization-id organization-id}))
          projects (-> result :body :projects)
          names    (into #{} (map :name projects))]
      (contains? names name))))

(defn- get-projects [organization-id & options]
  (when (:debug @app-state) (println "calling get-projects"))
  (read-request :get-projects {:organization-id organization-id}))

(defn- print-projects [organization-id & options]
  #_{:pre [(assert (and (not (nil? (utils/get-env "ORGANIZATION_ID")))
                        (not (nil? (utils/get-env "APIKEY")))) "ORGANIZATION_ID, APIKEY not set")]}
  (when (:debug @app-state) (println "calling print-projects" organization-id))
  (go
    (let [result   (<! (get-projects organization-id options))
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

(defn create-project [organization-id & options]
  (when (:debug @app-state)  (println "calling create-project" organization-id))
  (go
    (when (<! (project-exist? {:organization-id organization-id :name name}))
      (do
        (println "Error: Project" name "already exists")
        nil)))
  (write-request :create-project {:organization-id organization-id} (first options)))

(defn delete-project [project-id & options]
  (println "calling delete-project" project-id)
  (when (:debug @app-state) (println "calling delete-project" project-id))
  (read-request :delete-project {:project-id project-id}))

(defn get-devices-organization [organization-id & options]
  (when (:debug @app-state) (println "calling get-devices-organization" organization-id))
  (read-request :get-devices-organization {:organization-id organization-id}))

(defn- print-devices-organization [organization-id & options]
  (when (:debug @app-state) (println "calling print-devices-organization" organization-id))
  (go
    (let [result  (<! (get-devices-organization organization-id (first options)))
          devices (-> result :body :devices)
          coll    (into []
                        (reduce
                         (fn [acc device]
                           (let [id               (:id device)
                                 facility         (-> device :facility :name)
                                 operating-system (-> device :operating_system :name)
                                 hostname         (:hostname device)
                                 tags             (if-not (str/blank? (first (:tags device)))
                                                    (into #{} (mapv #(str/trim %) (:tags device)))
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
          coll    (if-not (empty? (:filter  (first options)))
                    (utils/filter-pred coll  (:filter (first options)))
                    coll)]
      (when (empty? devices)
        (do (println "No Devices found for " organization-id)
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn get-devices-project [project-id & options]
  (when (:debug @app-state) (println "calling list-devices-project" project-id))
  (read-request :get-devices-project {:project-id project-id}))

(defn- print-devices-project  [project-id & options]
  (when (:debug @app-state) (println "calling list-devices" (first options)))
  (go
    (let [result (<! (get-devices-project project-id (first options)))
          devices (-> result :body :devices)
          coll    (into []
                        (reduce
                         (fn [acc device]
                           (let [id               (:id device)
                                 facility         (-> device :facility :name)
                                 operating-system (-> device :operating_system :name)
                                 hostname         (:hostname device)
                                 tags             (if-not (str/blank? (first (:tags device)))
                                                    (into #{} (mapv #(str/trim %) (:tags device)))
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
        (do (println "No Devices found for " project-id)
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn device-exist-project? [{:keys [project-id name]}]
  (when (:debug @app-state) (println "calling device-exists?" name))
  (go
    (let [result   (<! (get-devices-project project-id))
          devices (-> result :body :devices)
          names (into #{} (map :hostname devices))]
      (when (:debug @app-state) (println "Device-exists project" name project-id (contains? names name)))
      (contains? names name))))

(defn device-exist-organization? [{:keys [organization-id name]}]
  (when (:debug @app-state) (println "calling device-exists?" name))
  (go
    (let [result   (<! (get-devices-organization organization-id))
          devices (-> result :body :devices)
          names (into #{} (map :hostname devices))]
      (when (:debug @app-state) (println "Device-exists organization" name organization-id (contains? names name)))
      (contains? names name))))

(defn create-device [project-id {:keys [hostname facility tags plan operating_system] :as args}]
  (when (:debug @app-state)  (println "calling create-device" args))
  (go
    (if (<! (device-exist-project? {:project-id project-id :name hostname}))
        (println "Error: Device" hostname "already exists")
        (write-request :create-device {:project-id project-id} {:hostname         hostname
                                                          :facility         facility
                                                          :tags             [tags]
                                                          :plan             plan
                                                          :operating_system operating_system}))))

(defn get-device [device-id & options]
  (when (:debug @app-state) (println "calling get-device" device-id))
  (read-request :get-device {:device-id device-id}))

(defn- delete-device [device-id & options]
  (when (:debug @app-state) (println "calling delete-device"))
  (go
    (let [result (<! (get-device device-id ))
        state (-> result :body :state)]
    (if (= state "active")
      (read-request :delete-device {:device-id device-id})
      (println "Error: Device cannot be deleted while provisioning")))))

(defn- print-device [device-id & options]
  (when (:debug @app-state) (println "calling get-device"))
  (go
    (let [result (<! (get-device device-id))
          device (-> result :body)]
      (condp = (:output @app-state)
        :json  (utils/print-json device)
        :edn   (utils/print-edn device)
        :table (utils/print-table device)
        (utils/print-table device)))))

(defn- get-project [project-id]
  (when (:debug @app-state)  (println "calling get-project" project-id))
  (read-request :get-project {:project-id project-id}))

(defn- get-project-name [project-id]
  (when (:debug @app-state)  (println "calling get-project-name" project-id))
  (go
    (let [project (<! (read-request :get-project {:project-id project-id}))]
      (-> project :body :name))))

(defn- print-project [project-id] nil)

(defmethod api/print-organizations :packet [_] (print-organizations))
(defmethod api/get-organizations   :packet [_] (get-organizations))
(defmethod api/project-exist? :packet [_  & options] (project-exist? (first options)))
(defmethod api/get-projects :packet [_  organization-id & options] (get-projects organization-id (first options)))
(defmethod api/print-projects :packet [_ organization-id & options] (print-projects organization-id (first options)))
(defmethod api/create-project :packet [_ organization-id & options] (create-project organization-id (first options)))
(defmethod api/delete-project :packet [_  project-id & options] (delete-project project-id (first options)))
(defmethod api/get-devices-organization :packet [_ organization-id & options] (get-devices-organization organization-id (first options)))
(defmethod api/print-devices-organization :packet [_ organization-id & options] (print-devices-organization organization-id (first options)))
(defmethod api/print-devices-project :packet [_ project-id & options] (print-devices-project project-id (first options)))
(defmethod api/get-devices-project :packet [_ project-id & options] (get-devices-project project-id (first options)))
(defmethod api/device-exist? [:packet :organization] [_ project-or-organization & options] (device-exist-organization? (first options)))
(defmethod api/device-exist? [:packet :project] [_ project-or-organization & options] (device-exist-project? (first options)))
(defmethod api/create-device :packet [_ project-id & options] (create-device project-id (first options)))
(defmethod api/delete-device :packet [_ device-id & options] (delete-device device-id (first options)))
(defmethod api/print-device :packet [_ device-id & options] (print-device device-id (first options)))
(defmethod api/get-device :packet   [_ device-id & options] (get-device device-id (first options)))
(defmethod api/print-project :packet [_ & options] (print-project (first options)))
(defmethod api/get-project :packet  [_ & options] (get-project (first options)))
(defmethod api/get-project-name :packet  [_ project-id] (get-project-name project-id ))

(def exports #js {})



;; (defn- list-plans []
;;   (when (:debug @app-state) (println "calling list-plans" ))
;;     (go
;;       (let [plans (:plans (<! (read-request :get-plans)))
;;             coll    (into []
;;                           (reduce
;;                            (fn [acc m]
;;                              (conj acc (select-keys m [:id :name :slug :description ])))
;;                            []
;;                            plans))]
;;         (condp = (:output @app-state)
;;           :json (utils/print-json coll)
;;           :edn   (utils/print-edn coll)
;;           :table (utils/print-table coll)
;;           )
;;         coll)))

;; (defn create-service [args]
;;   nil)

;; (defn- strip-device-item [i]
;;   (let [device        (str (:href i))
;;         device-id     (last (re-find #"/\w+/(\S+)" device))]
;;     device-id))
