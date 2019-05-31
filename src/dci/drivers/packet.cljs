(ns dci.drivers.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [w3c-xmlhttprequest-plus :refer [XMLHttpRequest]]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :as pprint]
            [dci.state :refer [app-state]]
            [dci.drivers.interfaces :as api]
            [dci.utils.core :refer [log-error] :as utils]))

(set! js/XMLHttpRequest XMLHttpRequest) ;; weird status response
(def library-version "0.0.4")

(def add-authentication-header
  {:name  ::add-authentication-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "X-Auth-Token"] (utils/get-env "APIKEY")))})

(def add-proxy-header
  {:name  ::add-proxy-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers] {"Access-Control-Request-Headers" "Host"
                                               "Host"                         "api.packet.net"}))})
(defn bootstrap-packet-cljs []
  (martian-http/bootstrap "https://api.packet.net"
                          [{:route-name :get-plans
                            :path-parts ["/plans"]
                            :summary    "Get plans listing"
                            :method     :get}
                           {:route-name :get-facilities
                            :path-parts ["/facilities"]
                            :summary    "Get facilities listing"
                            :method     :get}
                           {:route-name  :get-operating-systems
                            :path-parts ["/operating-systems"]
                            :summary    "Get operating-system listing"
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
                            :body-schema {:device {:hostname         (s/maybe s/Str)
                                                   :facility         [s/Str]
                                                   :tags             [(s/maybe s/Str)]
                                                   :userdata         (s/maybe s/Str)
                                                   :plan             s/Str
                                                   :operating_system s/Str}}}
                           {:route-name  :create-project
                            :path-parts  ["/organizations/" :organization-id "/projects"]
                            :path-schema {:organization-id s/Str}
                            :summary     "Create Project"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                            :body-schema {:project {:name s/Str
                                                    :backend_transfer_enabled s/Bool}}}
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
                           {:route-name  :get-device-events
                            :path-parts  ["/devices/" :device-id "/events"]
                            :summary     "Get device event listing"
                            :method      :get
                            :path-schema {:device-id s/Str}}
                           {:route-name  :validate-userdata
                            :path-parts  ["/userdata/validate"]
                            :summary     "Validate userdata"
                            :method      :post
                            :body-schema {:userdata {:userdata s/Any}}}
                           {:route-name  :delete-device
                            :path-parts  ["/devices/" :device-id]
                            :summary     "Delete device"
                            :method      :delete
                            :path-schema {:device-id s/Str}}]
                          {:interceptors (concat
                                          [add-authentication-header]
                                          #_[add-proxy-header]
                                          martian-http/default-interceptors)}))

(defn response-handler [response]
    (when (:debug @app-state)
      (pprint/pprint (js->clj (:body response) true)))
  response)

(defn request-handler
  ([k path-m body]
   (when (:debug @app-state) (debug "read-request" k path-m body))
   (go
     (try
       (let [m      (bootstrap-packet-cljs)
             _      (when (:debug @app-state)
                      (debug (martian/request-for m (merge path-m {::martian/body body}))))
             response (<! (martian/response-for m k (merge path-m {::martian/body body})))]
         (response-handler response))
     (catch js/Error e
       (error "Request Error" path-m body e)))))
  ([k path-m]
   (request-handler k path-m nil))
  ([k]
   (request-handler k nil nil)))

(defn request-handler-2
  ([k body]
   (when (:debug @app-state) (debug "read-request" k  body))
   (go
     (try
       (let [m        (<! (martian-http/bootstrap-swagger "https://api.packet.net/api-docs/"))  #_(bootstrap-packet-cljs)
             _        (when (:debug @app-state)
                        (debug (martian/request-for m k body)))
             response (<! (martian/response-for m k body))]
         (response-handler response))
       (catch js/Error e
         (error "Request Error" k body e)))))
  ([k]
   (request-handler k nil)))

;; Implementations


(defn- get-organizations []
  (when (:debug @app-state) (debug "calling get-organizations"))
  (request-handler :get-organizations))

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
        (do (error "No Organizations found for user")
            (.exit js/process 0)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn- get-facilities []
  (when (:debug @app-state) (debug "calling get-facilities"))
  (request-handler :get-facilities))

(defn- print-facilities []
  (go
    (let [result        (<! (get-facilities))
          organizations (-> result :body :facilities)
          coll          (into []
                              (reduce
                               (fn [acc m]
                                 (conj acc (select-keys m [:code :name :features])))
                               []
                               organizations))]
      (when (empty? organizations)
        (error "No Facilities found"))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))



(defn project-exist-id? [{:keys [project-id]}]
  (when (:debug @app-state) (debug "calling project-exists?" project-id))
  (go
    (let [result    (<! (request-handler :get-project {:project-id project-id}))]
      (condp = (:status result)
        200 true
        false))))

(defn project-exist-name? [{:keys [organization-id name]}]
  (when (:debug @app-state) (debug "calling project-exists?" name))
  (go
    (let [result    (<! (request-handler :get-projects {:organization-id organization-id}))
          projects  (-> result :body :projects)
          project-m (filterv #(= (:name %) name) projects)]
      (if-not (empty? project-m)
        true
        false))))

(defn- get-projects [organization-id & options]
  (when (:debug @app-state) (debug "calling get-projects"))
  (request-handler :get-projects {:organization-id organization-id}))

(defn- print-projects [organization-id & options]
  #_{:pre [(assert (and (not (nil? (utils/get-env "ORGANIZATION_ID")))
                        (not (nil? (utils/get-env "APIKEY")))) "ORGANIZATION_ID, APIKEY not set")]}
  (when (:debug @app-state) (debug "calling print-projects" organization-id))
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
        (do (info "No Projects found for user")
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn validate-userdata [userdata]
  (debug userdata)
  (swap! app-state assoc :debug true)
  (go
    (let [userdata (<! (request-handler :validate-userdata nil {:userdata userdata}))]
      (debug userdata))))

(defn create-project [organization-id project-name & options]
  (when (:debug @app-state)  (debug "calling create-project" organization-id project-name options))
  (go
    (let [project? (<! (project-exist-name? {:organization-id organization-id :name project-name}))]
      (when (:debug @app-state)  (debug "calling create-project" project?))
      (if project?
        (do
          (error "Project: Name:" project-name  "exists")
          nil)
        (let [project-payload {:organization-id organization-id
                               :project {:name project-name}}
              project-payload-merge (if-some [options (first options)]
                                      (spy (update-in project-payload [:project] conj options))
                                      project-payload
                                      )
              project        (<! (request-handler :create-project project-payload-merge))
              project-id     (-> project :body :id)
              project-record (conj [] (select-keys (:body project) [:id :name :created_at]))]
          (do
            (utils/print-table project-record)
            project-id))))))

(defn delete-project [project-id & options]
  (when (:debug @app-state) (debug "calling delete-project" project-id))
  (go
    (let [project (<! (request-handler :delete-project {:project-id project-id}))]
      (condp = (:status project)
        204 (error "Project" project-id "deleted")
        404 (error "Project" project-id "doesn't exist")
        422 (error "Project" project-id (first (:errors (:body project))))
        (error "Error deleting project" project-id)))))

(defn get-devices-organization [organization-id & options]
  (when (:debug @app-state) (debug "calling get-devices-organization" organization-id))
  (go
    (let [result (<! (request-handler :get-devices-organization {:organization-id organization-id}))]
      (when (:debug @app-state) (debug "get-devices-organization result" result))
      (if (= (:status result) 200)
        (-> result :body :devices)
        nil))))

(defn- print-devices-organization [organization-id & options]
  (when (:debug @app-state) (debug "calling print-devices-organization" organization-id))
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
        (do (info "No Devices found for " organization-id)
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn get-devices-project [project-id & options]
  (when (:debug @app-state) (debug "calling get-devices-project" project-id))
  (go
    (try
      (let [result (<! (request-handler :get-devices-project {:project-id project-id}))]
        (if (= (:status result) 200)
          (let [devices (-> result :body :devices)]
                (if-not (empty? (:filter (first options)))
                          (utils/filter-pred devices (:filter (first options)))
                          devices))
          (throw (js/Error. (:error result)))))
      (catch js/Error e
        e))))

(defn- print-devices-project  [project-id options]
  (when (:debug @app-state) (debug "calling print-devices-project" project-id (first options)))
  (go
    (let [devices (<! (get-devices-project project-id))
          coll    (into []
                        (reduce
                         (fn [acc device]
                           (let [id               (:id device)
                                 facility         (-> device :facility :name)
                                 operating-system (-> device :operating_system :name)
                                 hostname         (:hostname device)
                                 tags             (when-not (str/blank? (first (:tags device)))
                                                    (into #{} (mapv #(str/trim %) (:tags device))))
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
          coll    (if-not (empty? (:filter (first options)))
                    (utils/filter-pred coll (:filter (first options)))
                    coll)]
      (when (empty? devices)
        (do (info "No Devices found for " project-id)
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn device-exist-id? [{:keys [device-id]}]
  (when (:debug @app-state) (debug "calling device-exists-id?" name))
  (go
    (let [result  (<! (request-handler :get-device {:device-id device-id}))]
      (condp = (:status result)
        200 true
        false))))

(defn device-exist-project? [project-id name]
  (when (:debug @app-state) (debug "calling device-exists?" project-id name))
  (go
    (let [devices  (<! (get-devices-project project-id))
          device-m (filterv #(= (:hostname %) name) devices)]
            ;names   (into #{} (map :hostname devices))]
      (if-not (empty? device-m)
        true
        false))))

(defn device-exist-organization? [{:keys [organization-id name]}]
  (when (:debug @app-state) (debug "calling device-exists?" name))
  (go
    (let [result   (<! (get-devices-organization organization-id))
          devices (-> result :body :devices)
          device-m (filterv #(= (:name %) name) devices)]
         ; names (into #{} (map :hostname devices))]
      (when (:debug @app-state) (debug "Device-exists organization" name organization-id))
      (if-not (empty? device-m)
        (-> device-m first :id)
        nil))))
      ;(contains? names name))))

(defn create-device [project-id {:keys [hostname facility tags plan operating_system distribute userdata] :as args}]
  (when (:debug @app-state)  (debug "calling create-device" project-id args))
  (go
    (let [device-id? (<! (device-exist-project? project-id hostname))]
      (if device-id?
        (error "Device: Name:" hostname "exists")
        (let [device        (<! (request-handler
                                 :create-device {:project-id project-id
                                                 :device     {:hostname         hostname
                                                              :facility         facility
                                                              :tags             tags
                                                              :plan             plan
                                                              :userdata         (or userdata "")
                                                              :operating_system operating_system}}))
              device-record (conj [] (select-keys (:body device) [:id :hostname :created_at]))]
          (when (:debug @app-state)  (debug "result create device" device))
          (info device-record))))))



(defn get-device-id [device-id & options]
  (when (:debug @app-state) (debug "calling get-device" device-id))
  (go
    (let [result (<! (request-handler :get-device {:device-id device-id}))]
    (if (= (:status result) 200)
      (-> result :body)
      nil))))


(defn get-device [organization-id device & options]
  (when (:debug @app-state) (debug "calling get-device" device))
  (go
    (if-some [id (re-find #"\w{8}\-\w{4}\-\w{4}\-\w{4}\-\w{12}" device)]
      (get-device-id device)
      (if-some [devices (<! (get-devices-organization organization-id))]
        (let [;device-vec (mapv #(select-keys % [:id :hostname]) devices)
              device-obj (first (for [dev   devices
                                      :when (= (:hostname dev) device)]
                                  dev))]
        device-obj)))))


(defn- delete-device [device-id & options]
  (when (:debug @app-state) (debug "calling delete-device"))
  (go
    (if-some [device (<! (get-device-id device-id))]
      (if (= (:state device) "active")
        (let [device (<! (request-handler :delete-device {:device-id device-id}))]
          (condp = (:status device)
            204 (info "Device" device-id "deleted")
            404 (error "Device" device-id "doesn't exist")
            (error  "Error deleting device" device-id)))
        (error "Error: Device cannot be deleted while provisioning")))))

(defn- print-device [device-id & options]
  (when (:debug @app-state) (debug "calling get-device"))
  (go
    (let [result (<! (get-device-id device-id))
          device (-> result :body)]
      (condp = (:output @app-state)
        :json  (utils/print-json device)
        :edn   (utils/print-edn device)
        :table (utils/print-table device)
        (utils/print-table device)))))

(defn get-deviceid-prefix [organization-id prefix]
  (when (:debug @app-state) (debug "calling project-exists?" prefix))
  (go
    (let [devices (<! (get-devices-organization organization-id))
          device-ids (into #{} (mapv :id devices))
          device-selector  (utils/prefix-match prefix device-ids)]
      (when (:debug @app-state) (debug "device-selector" device-selector))
      (if (some? device-selector)
        device-selector
        (js/Error. "Device" prefix "not found" )))))

(defn- get-device-events [device-id & options]
  (when (:debug @app-state)  (debug "calling get-device-events " device-id))
  (go
    (let [device (<! (request-handler :get-device-events {:device-id device-id}))]
      (condp = (:status device)
        200 (-> device :body :events)
        404 (error "Device" device-id "doesnt exist")
        (error "Error retrieving device" device-id)))))

(defn- get-project [project-id & options]
  (when (:debug @app-state)  (debug "calling get-project" project-id))
  (go
    (let [project (<! (request-handler :get-project {:project-id project-id}))]
      (condp = (:status project)
        200 (:body project)
        404 (error "Project" project-id "doesnt exist")
        (error "Error retrieving project" project-id)))))

(defn- get-project-id
  "Return a project id or nil if project-name exists"
  [organization-id project-name]
  (when (:debug @app-state)  (debug "calling get-project-id" project-name))
  (go
    (let [result      (<! (request-handler :get-projects {:organization-id organization-id}))
          projects    (-> result :body :projects)
          sel-project (filterv #(= (:name %) project-name) projects)
          project-id  (-> sel-project first :id)]
      (when (:debug @app-state) (debug "get-project-id" project-name project-id))
      (if (some? project-id)
        project-id
        nil)
      )))

(defn- get-project-name [project-id]
  (when (:debug @app-state)  (debug "calling get-project-name" project-id))
  (go
    (let [project (<! (request-handler :get-project {:project-id project-id}))]
      (condp = (:status project)
        200 (-> project :body :name)
        (error "Error retrieving project" project-id)))))

(defn get-projectid-prefix [organization-id prefix]
  (when (:debug @app-state) (debug "calling project-exists?" prefix))
  (go
    (let [result (<! (get-projects organization-id))
          projects (-> result :body :projects)
          project-ids (into #{} (mapv :id projects))
          project-selector  (utils/prefix-match prefix project-ids)]
      (when (:debug @app-state) (debug "project-selector" project-selector))
      (if (some? project-selector)
        project-selector
        nil))))

(defn gen-inventory [organization-id service-name]
  (when (:debug @app-state) (debug "calling get-inventory" organization-id service-name))
  (go
    (let [inventory  {:group {} :_meta {:hostvars {}}}
          project-id (<! (get-project-id organization-id service-name))
          result     (<! (get-devices-project project-id))
          devices    (-> result :body :devices)
          coll       (reduce
                      (fn [acc device]
                        (let [id        (:id device)
                              hostname  (:hostname device)
                              tags      (when-not (str/blank? (first (:tags device)))
                                          (into #{} (mapv #(str/trim %) (:tags device))))
                              addresses (into #{} (mapv #(:address %) (:ip_addresses device)))
                              state     (:state device)]
                          (conj acc (first addresses))))
                                        ;{:id               id
                                        ; :facility         facility
                                        ; :operating-system operating-system
                                        ; :hostname         hostname
                                        ; :addresses        addresses
                                        ; :root-password    root-password
                                        ; :state            state
                                        ; :tags             tags})))
                      []
                      devices)
          hosts (assoc-in inventory [:group :hosts] coll)
          ;hostvars (assoc-in hosts [:_meta :hostvars] coll)
          ]

      (info (utils/print-json hosts)))))

(defn- print-project [project-id] nil)

(defmethod api/print-facilities :packet [_] (print-facilities))
(defmethod api/get-facilities :packet [_] (get-facilities))
(defmethod api/print-organizations :packet [_] (print-organizations))
(defmethod api/get-organizations   :packet [_] (get-organizations))
(defmethod api/project-exist? [:packet :id] [_ id-or-name & options] (project-exist-id? (first options)))
(defmethod api/project-exist? [:packet :name]  [_  id-or-name & options] (project-exist-name? (first options)))
(defmethod api/get-projectid-prefix :packet [_ organization-id prefix] (get-projectid-prefix organization-id prefix))
(defmethod api/get-projects :packet [_  organization-id & options] (get-projects organization-id (first options)))
(defmethod api/print-projects :packet [_ organization-id & options] (print-projects organization-id (first options)))
(defmethod api/create-project :packet [_ organization-id project-name & options] (create-project organization-id project-name (first options)))
(defmethod api/delete-project :packet [_  project-id & options] (delete-project project-id (first options)))
(defmethod api/get-devices-organization :packet [_ organization-id & options] (get-devices-organization organization-id  options))
(defmethod api/print-devices-organization :packet [_ organization-id & options] (print-devices-organization organization-id (first options)))
(defmethod api/print-devices-project :packet [_ project-id & options] (print-devices-project project-id options))
(defmethod api/get-devices-project :packet [_ project-id & options] (get-devices-project project-id options))
(defmethod api/device-exist? [:packet :organization] [_ project-or-organization & options] (device-exist-organization? (first options)))
(defmethod api/device-exist? [:packet :project] [_ project-or-organization project-id name & options] (device-exist-project? project-id name ))
(defmethod api/create-device :packet [_ project-id & options] (create-device project-id (first options)))
(defmethod api/delete-device :packet [_ project-id device-id & options] (delete-device project-id device-id options))
(defmethod api/print-device :packet [_ device-id & options] (print-device device-id (first options)))
(defmethod api/get-deviceid-prefix :packet [_ organization-id prefix] (get-deviceid-prefix organization-id prefix))
(defmethod api/get-device :packet   [_ organization-id device-id & options] (get-device organization-id device-id first options))
(defmethod api/get-device-events :packet   [_ device-id & options] (get-device-events device-id first options))
(defmethod api/print-device-events :packet   [_ device-id & options] (get-device-events device-id first options))
(defmethod api/print-project :packet [_ & options] (print-project (first options)))
(defmethod api/get-project :packet  [_  project-id & options] (get-project project-id (first options)))
(defmethod api/get-project-name :packet  [_ project-id] (get-project-name project-id))
(defmethod api/get-project-id :packet [_ organization-id project-name] (get-project-id organization-id project-name))
(defmethod api/gen-inventory :packet [_ organization-id project-name] (gen-inventory organization-id project-name))

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
