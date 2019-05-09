(ns dci.drivers.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [w3c-xmlhttprequest-plus :refer [XMLHttpRequest]]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :as pprint]
            [dci.state :refer [app-state]]
            [dci.drivers.interfaces :as api]
            [dci.utils.core :refer [log-error] :as utils]))

;(set! js/XMLHttpRequest xhr2)
(set! js/XMLHttpRequest XMLHttpRequest) ;; weird status response


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
                            :body-schema {:device {:hostname         s/Str
                                                   :facility         [s/Str]
                                                   :tags             [(s/maybe s/Str)]
                                                   :plan             s/Str
                                                   :operating_system s/Str}}}
                           {:route-name  :create-project
                            :path-parts  ["/organizations/" :organization-id "/projects"]
                            :path-schema {:organization-id s/Str}
                            :summary     "Create Project"
                            :method      :post
                            :produces    ["application/json"]
                            :consumes    ["application/json"]
                                        ;:query-params {:exclude ["devices" "members" "memberships"
                                        ;                         "invitations" "max_devices" "ssh_keys"]}
                            :body-schema {:project {:name s/Str}}}
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
                                          #_[add-proxy-header]
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
      404 {:status 404 :body body}
      406 {:status 406 :body body}
      422 {:status 422 :body body}
      200 {:status 200 :body body}
      201 {:status 201 :body body}
      204 {:status 204 :body body}
      (error-and-exit))))

(defn write-request [k body]
  (when (:debug @app-state) (println "write-request" k body))
  (go  (let [m    (bootstrap-packet-cljs)
             _    (when (:debug @app-state)
                    (do
                      (println "DryRun" (martian/request-for m k body))))
             response (<! (martian/response-for m k body))]
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

(defn project-exist-id? [{:keys [project-id]}]
  (when (:debug @app-state) (println "calling project-exists?" project-id))
  (go
    (let [result    (<! (read-request :get-project {:project-id project-id}))]
      (condp = (:status result)
        200 true
        false))))

(defn project-exist-name? [{:keys [organization-id name]}]
  (when (:debug @app-state) (println "calling project-exists?" name))
  (go
    (let [result    (<! (read-request :get-projects {:organization-id organization-id}))
          projects  (-> result :body :projects)
          project-m (filterv #(= (:name %) name) projects)]
      (if-not (empty? project-m)
        true
        false))))

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

(defn create-project [organization-id project-name & options]
  (when (:debug @app-state)  (println "calling create-project" organization-id project-name))
  (go
    (let [project? (<! (project-exist-name? {:organization-id organization-id :name project-name}))]
      (when (:debug @app-state)  (println "calling create-project" project?))
      (if project?
        (do
          (log-error "Project: Name:" project-name  "exists")
          nil)
        (let [project        (<! (write-request :create-project {:organization-id organization-id
                                                                 :name            project-name}))
              project-id     (-> project :body :id)
              project-record (conj [] (select-keys (:body project) [:id :name :created_at]))]
          (do
            (utils/print-table project-record)
            (project-id)))))))

(defn delete-project [project-id & options]
  (when (:debug @app-state) (println "calling delete-project" project-id))
  (go
    (let [project (<! (read-request :delete-project {:project-id project-id}))]
      (condp = (:status project)
        204 (log-error "Project" project-id "deleted")
        404 (log-error "Project" project-id "doesn't exist")
        422 (log-error "Project" project-id (first (:errors (:body project))))
        (log-error "js/console.error" "Error deleting project" project-id)))))

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
  (when (:debug @app-state) (println "calling get-devices-project" project-id))
  (read-request :get-devices-project {:project-id project-id}))

(defn- print-devices-project  [project-id options]
  (when (:debug @app-state) (println "calling print-devices-project"  options))
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
          coll    (if-not (empty? (:filter options))
                    (utils/filter-pred coll (:filter options))
                    coll)]
      (when (empty? devices)
        (do (println "No Devices found for " project-id)
            (.exit js/process)))
      (condp = (:output @app-state)
        :json  (utils/print-json coll)
        :edn   (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn device-exist-id? [{:keys [device-id]}]
  (when (:debug @app-state) (println "calling device-exists-id?" name))
  (go
    (let [result  (<! (read-request :get-device {:device-id device-id}))]
      (condp = (:status result)
        200 true
        false))))

(defn device-exist-project? [{:keys [project-id name]}]
  (when (:debug @app-state) (println "calling device-exists?" name))
  (go
    (let [result  (<! (get-devices-project project-id))
          devices (-> result :body :devices)
          device-m (filterv #(= (:hostname %) name) devices)]
            ;names   (into #{} (map :hostname devices))]
      (when (:debug @app-state) (println "Device-exists project" name project-id))
      (if-not (empty? device-m)
        true
        false))))

        ;(contains? names name))))

(defn device-exist-organization? [{:keys [organization-id name]}]
  (when (:debug @app-state) (println "calling device-exists?" name))
  (go
    (let [result   (<! (get-devices-organization organization-id))
          devices (-> result :body :devices)
          device-m (filterv #(= (:name %) name) devices)]
         ; names (into #{} (map :hostname devices))]
      (when (:debug @app-state) (println "Device-exists organization" name organization-id))
      (if-not (empty? device-m)
        (-> device-m first :id)
        nil))))
      ;(contains? names name))))

(defn create-device [project-id {:keys [hostname facility tags plan operating_system] :as args}]
  (when (:debug @app-state)  (println "calling create-device" project-id args))
  (go
    (let [device-id? (<! (device-exist-project? {:project-id project-id :name hostname}))]
      (if device-id?
        (log-error "Device: Name:" hostname "exists")
        (let [device (<! (write-request :create-device {:project-id project-id
                                                        :device     {:hostname         hostname
                                                                     :facility         facility
                                                                     :tags             [tags]
                                                                     :plan             plan
                                                                     :operating_system operating_system}}))
              device-record (conj [] (select-keys (:body device) [:id :hostname :created_at]))]
          (when (:debug @app-state)  (println "result create device" device))
          (utils/print-table device-record))))))

(defn get-device [device-id & options]
  (when (:debug @app-state) (println "calling get-device" device-id))
  (read-request :get-device {:device-id device-id}))

(defn get-device-id [device-id & options]
  (when (:debug @app-state) (println "calling get-device" device-id))
  (read-request :get-device {:device-id device-id}))

(defn- delete-device [device-id & options]
  (when (:debug @app-state) (println "calling delete-device"))
  (go
    (let [result (<! (get-device device-id))
          state (-> result :body :state)]
      (if (= state "active")
        (let [device (<! (read-request :delete-device {:device-id device-id}))]
          (condp = (:status device)
            204 (log-error "Device" device-id "deleted")
            404 (log-error "Device" device-id "doesn't exist")
            (log-error "js/console.error" "Error deleting device" device-id)))
        (log-error "Error: Device cannot be deleted while provisioning")))))

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

(defn get-deviceid-prefix [organization-id prefix]
  (when (:debug @app-state) (println "calling project-exists?" prefix))
  (go
    (let [result (<! (get-devices-organization organization-id))
          devices (-> result :body :devices)
          device-ids (into #{} (mapv :id devices))
          device-selector  (utils/prefix-match prefix device-ids)]
      (when (:debug @app-state) (println "device-selector" device-selector))
      (if (some? device-selector)
        device-selector
        nil
        nil))))

(defn- get-project [project-id & options]
  (when (:debug @app-state)  (println "calling get-project" project-id))
  (go
    (let [project (<! (read-request :get-project {:project-id project-id}))]
      (condp = (:status project)
        200 (:body project)
        404 (log-error "Project" project-id "doesnt exist")
        (log-error "Error retrieving project" project-id)))))

(defn- get-project-id [organization-id project-name]
  (when (:debug @app-state)  (println "calling get-project-id" project-name))
  (go
    (let [result      (<! (read-request :get-projects {:organization-id organization-id}))
          projects    (-> result :body :projects)
          sel-project (filterv #(= (:name %) project-name) projects)
          project-id (-> sel-project first :id)]
      (when (:debug @app-state) (println "get-project-id" project-name project-id))
      project-id)))

(defn- get-project-name [project-id]
  (when (:debug @app-state)  (println "calling get-project-name" project-id))
  (go
    (let [project (<! (read-request :get-project {:project-id project-id}))]
      (condp = (:status project)
        200 (-> project :body :name)
        403 nil
        (log-error "Error retrieving project" project-id)))))

(defn get-projectid-prefix [organization-id prefix]
  (when (:debug @app-state) (println "calling project-exists?" prefix))
  (go
    (let [result (<! (get-projects organization-id))
          projects (-> result :body :projects)
          project-ids (into #{} (mapv :id projects))
          project-selector  (utils/prefix-match prefix project-ids)]
      (when (:debug @app-state) (println "project-selector" project-selector))
      (if (some? project-selector)
        project-selector
        (<! (get-project prefix))
        nil))))

(defn- print-project [project-id] nil)

(defmethod api/print-organizations :packet [_] (print-organizations))
(defmethod api/get-organizations   :packet [_] (get-organizations))
(defmethod api/project-exist? [:packet :id] [_ id-or-name & options] (project-exist-id? (first options)))
(defmethod api/project-exist? [:packet :name]  [_  id-or-name & options] (project-exist-name? (first options)))
(defmethod api/get-projectid-prefix :packet [_ organization-id prefix] (get-projectid-prefix organization-id prefix))
(defmethod api/get-projects :packet [_  organization-id & options] (get-projects organization-id (first options)))
(defmethod api/print-projects :packet [_ organization-id & options] (print-projects organization-id (first options)))
(defmethod api/create-project :packet [_ organization-id project-name & options] (create-project organization-id project-name (first options)))
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
(defmethod api/get-deviceid-prefix :packet [_ organization-id prefix] (get-deviceid-prefix organization-id prefix))
(defmethod api/get-device :packet   [_ device-id & options] (get-device device-id (first options)))
(defmethod api/print-project :packet [_ & options] (print-project (first options)))
(defmethod api/get-project :packet  [_  project-id & options] (get-project project-id (first options)))
(defmethod api/get-project-name :packet  [_ project-id] (get-project-name project-id))
(defmethod api/get-project-id :packet [_ organization-id project-name] (get-project-id organization-id project-name))
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
