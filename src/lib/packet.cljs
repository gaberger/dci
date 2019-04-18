(ns lib.packet
  (:require [martian.core :as martian]
            [martian.cljs-http :as martian-http]
            [util]
            [schema.core :as s :include-macros true]
            [cljs.core.async :refer [<! timeout take!] :refer-macros [go go-loop]]
            [clojure.string :as str]
            [clojure.pprint :as pprint]
            [xhr2]
            [server.dci-model :refer [IServer]]
            [dci.state :refer [app-state]]
            ))

(set! js/XMLHttpRequest xhr2)
(declare print-json)

(def add-authentication-header
  {:name ::add-authentication-header
   :enter (fn [ctx]
            (assoc-in ctx [:request :headers "X-Auth-Token"] (:apikey @app-state)))})

(defn bootstrap-packet-cljs []
  (martian-http/bootstrap "https://api.packet.net"
                          [
                           {:route-name  :get-projects
                            :path-parts  ["/projects/" :id]
                            :summary     "Get projects listing"
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
                                          :plan             s/Str
                                          :operating_system s/Str}
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

(defn read-request [k opts]
  (when (:debug @app-state) (println "read-request" k opts))
  (go  (let [m (bootstrap-packet-cljs)
             _ (when (:debug @app-state) (println (martian/request-for m k opts)))
             response (<! (martian/response-for m k opts))]
            (response-handler response))))


(defn get-projects [id]
  (go (let [m        (bootstrap-packet-cljs)
            response (<! (martian/response-for m :get-projects {:id id}))]
        (when (:debug @app-state) (println (martian/request-for m :get-projects {:id id})))
        (js/console.log "DEBUG" (.inspect util response))
        (response-handler response))))

(defn get-device [id]
  (read-request :get-device {:id id}))

(defn get-devices [args]
 (go
   (let [devices    (:devices (<! (read-request :get-devices args)))
         dev-vector (into []
                          (reduce
                           (fn [acc device]
                                  (let [id               (:id device)
                                        facility         (-> device :facility :name)
                                        operating-system (-> device :operating_system :name)
                                        hostname         (:hostname device)
                                        network          (:network device)
                                        root-password    (:root_password device)
                                        state            (:state device)]
                                    (conj acc {:id       id       :facility facility :operating-system operating-system
                                               :hostname hostname :network  network  :root-password    root-password
                                               :state    state})))
                                  []
                                  devices))]
    (when (empty? devices)
      (do (println "No devices found for project " (:id args))
          (.exit js/process)))
    (if  (:json @app-state)
      (print-json dev-vector)
      (pprint/print-table dev-vector)))))

(defn create-device [args]
    (write-request :create-device args))

(defn delete-device [id]
  (go (let [m (bootstrap-packet-cljs)
            response (<! (martian/response-for m :delete-device {:id id}))]
        (when (:debug @app-state) (println (martian/request-for m :delete-device {:id id})))
        (response-handler response))))

(defn- strip-device-item [i]
  (let [device        (str (:href i))
        device-id     (last (re-find #"/\w+/(\S+)" device))]
    device-id))


(defn- list-devices
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
  (list-servers [this project-id]
    (get-devices {:id project-id}))
  (create-server [this args]
     (create-device args))
  (delete-server  [this device-id] (delete-device device-id))
  (start-server  [this device-id] (println "start-server" device-id))
  (stop-server  [this device-id] (println "stop-server" name)))


(defn print-json [obj]
  (println (.stringify js/JSON (clj->js obj) nil " ")))
