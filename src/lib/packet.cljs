(ns lib.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint :as pprint]
            [xmlhttprequest :refer [XMLHttpRequest]]
            [server.dci-model :refer [IServer]]
            [dci.state :refer [app-state]]
            [utils.core :as utils]
            ))

(set! js/XMLHttpRequest XMLHttpRequest)
(declare print-json)

(def add-authentication-header
  {:name  ::add-authentication-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "X-Auth-Token"] (utils/get-env "APIKEY")))})


(defn bootstrap-packet-cljs []
  (martian-http/bootstrap "https://api.packet.net"
                          [
                           {:route-name :get-organizations
                            :path-parts ["/organizations"]
                            :summary    "Get Organization listing"
                            :method     :get
                            }
                           {:route-name :get-projects
                            :path-parts ["/organizations/" :id "/projects"]
                            :summary    "Get projects listing"
                            :method     :get
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
                                          :facility         [s/Str]
                                          :tags             [s/Str]
                                          :plan             s/Str
                                          :operating_system s/Str}
                            }  {:route-name  :create-project
                                :path-parts  ["/organizations/" :id "/project"]
                                :path-schema {:id s/Str}
                                :summary     "Create Project"
                                :method      :post
                                :produces    ["application/json"]
                                :consumes    ["application/json"]
                                :body-schema {:name              s/Any
                                              :payment_method_id [s/Str]
                                              :customdata        [s/Str]}
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
        (println "Status" status)
        (println "Debug: Body")
        (pprint/pprint (js->clj body true))))

  (condp = status
    401 (error-and-exit)
    404 (error-and-exit)
    422 (error-and-exit)
    406 body
    200 body
    201 body
    204 body
    (error-and-exit)
    )))

(defn write-request [k opts]
  (when (:debug @app-state) (println "write-request" k opts))
  (go  (let [m        (bootstrap-packet-cljs)
             id       (:id opts)
             opts     (dissoc opts :id)
             _        (when (:debug @app-state) (println (martian/request-for m k {:id            id
                                                                                   ::martian/body opts})))
             response (<! (martian/response-for m k {:id            id
                                                     ::martian/body opts}))]
                (response-handler response))))

(defn read-request
  ([k opts]
  (when (:debug @app-state) (println "read-request" k opts))
  (go  (let [m (bootstrap-packet-cljs)
             _ (when (:debug @app-state) (println (martian/request-for m k opts)))
             response (<! (martian/response-for m k opts))]
         (response-handler response))))
  ([k]
   (read-request k nil)))



;; Implementations

(defn get-device [id]
  (read-request :get-device {:id id}))

(defn- list-devices [args]
  (when (:debug @app-state) (println "calling list-devices" args))
 (go
   (let [id (first args)
         devices (:devices (<! (read-request :get-devices {:id id})))
         options (last args)
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
                            (conj acc {:id       id       :facility  facility  :operating-system operating-system
                                       :hostname hostname :addresses addresses :root-password    root-password
                                       :state    state    :tags      tags})))
                        []
                        devices))
         coll    (if-not (empty? (:filter options))
                   (utils/filter-pred coll (:filter options))
                   coll)]
     (when (empty? devices)
       (do (println "No Devices found for " id)
           (.exit js/process)))

     (condp = (:output @app-state)
       :json  (utils/print-json coll)
       :edn   (utils/print-edn coll)
       :table (utils/print-table coll)
       (utils/print-table coll)))))

(defn- list-projects []
  (when (:debug @app-state) (println "calling list-projects" ))
 (go
   (let [org-id (utils/get-env "ORGANIZATION_ID")
         projects (:projects  (<! (read-request :get-projects {:id org-id})))
         coll   (into []
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
        :json (utils/print-json coll)
        :edn (utils/print-edn coll)
        :table (utils/print-table coll)
        (utils/print-table coll)))))

(defn- list-organizations []
  (when (:debug @app-state) (println "calling list-orgnizations" ))
  (let [chan (chan)]
    (go
      (let [organizations (:organizations (<! (read-request :get-organizations)))
          coll    (into []
                          (reduce
                           (fn [acc m]
                             (conj acc (select-keys m [:id :name :description :account_id :monthly_spend])))
                           []
                           organizations))]
            (when (empty? organizations)
                   (do (println "No Organizations found for user")
                       (.exit js/process 0)))
            (condp = (:output @app-state)
                :json (utils/print-json coll)
                :edn   (utils/print-edn coll)
                :table (utils/print-table coll)
            (>! chan coll))))
    chan))



(defn- get-project-name [id]
  (when (:debug @app-state)  (println "calling get-project-name" id))
  (let [out-chan (chan)]
    (go
      (let [m        (bootstrap-packet-cljs)
            response (<! (read-request :get-project {:id id}))]
        (when (:debug @app-state) (println (martian/request-for m :get-project {:id id})))
        (>! out-chan (:name response))))
    out-chan))

;;TODO Check for duplicate host-name or use a generating uuid
(defn create-device [args]
  (when (:debug @app-state)  (println "calling create-device" args))
    (write-request :create-device (first args)))

(defn delete-device [id]
  (when (:debug @app-state) (println "calling delete-device" id ))
  (go (let [m (bootstrap-packet-cljs)
            response (<! (martian/response-for m :delete-device {:id id}))]
        (when (:debug @app-state) (println (martian/request-for m :delete-device {:id id})))
        (response-handler response))))

(defn- strip-device-item [i]
  (let [device        (str (:href i))
        device-id     (last (re-find #"/\w+/(\S+)" device))]
    device-id))


#_(defn- list-devices
  [project-id]
  (when (:debug @app-state) (println {:function   :list-devices
                                      :project-id project-id
                                      :app-state  @app-state}))
    (go
      (let [devices    (:devices (<! (get-devices project-id)))
            dev-vector (mapv #(select-keys % [:id :hostname :state :network]) devices)]
        (if (empty? devices)
          (println "No devices found for project " project-id)
          (.exit js/process))
        (if  (:json @app-state)
           (print-json dev-vector)
           (pprint/print-table dev-vector)))))

(deftype PacketServer []
  IServer
  (list-projects [this] (list-projects))
  (list-organizations [this] (list-organizations))
  (get-project-name [this args] (get-project-name (first args)))
  (list-servers [this args] (list-devices args))
  (create-server [this args] (create-device  args))
  (delete-server  [this args] (delete-device (first args)))
  (start-server  [this device-id] (println "start-server" device-id))
  (stop-server  [this device-id] (println "stop-server" name)))

