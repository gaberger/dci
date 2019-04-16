(ns lib.packet-test
  (:require [martian.cljs-http :as martian-http]
            [martian.core :as martian]
            [lib.packet :as packet]
            [server.dci-server :refer [get-api-token]]
            [xhr2]
            [cljs.test :refer-macros [deftest testing is run-tests async]]
            [cljs.core.async :refer [<! timeout take!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(set! js/XMLHttpRequest xhr2)

(def id "e40d191d-4394-477c-b47d-2f08cc26a98a")
(get-api-token)

(deftest get-devices-request
  (let [m        (packet/bootstrap-packet-cljs)
        response (martian/request-for m :get-devices {:id id})]
    (is (= {:headers {"X-Auth-Token" nil},
            :method  :get,
            :url     "https://api.packet.net/projects/e40d191d-4394-477c-b47d-2f08cc26a98a/devices",
            :as      :auto}
           response))))

(deftest create-device-request
  (let [m        (packet/bootstrap-packet-cljs)
        data  {:id               "e40d191d-4394-477c-b47d-2f08cc26a98a"
               :device           "foobar"
               :plan             "baremetal_0"
               :facility         ["ewr"]
               :operating_system "ubuntu_16_04"}
        id      (:id data)
        opts    (dissoc data :id)
        response (martian/request-for m :create-device {:id id
                                                        ::martian/body opts})]
    (is (= {:headers {"X-Auth-Token" nil, "Content-Type" "application/json", "Accept" "application/json"},
           :method :post, :url "https://api.packet.net/projects/e40d191d-4394-477c-b47d-2f08cc26a98a/devices",
           :body "{\"device\":\"foobar\",\"plan\":\"baremetal_0\",\"facility\":[\"ewr\"],\"operating_system\":\"ubuntu_16_04\"}", :as :text})
           response)))

(deftest create-device
  (async done
         (go (let [data    {:id               "e40d191d-4394-477c-b47d-2f08cc26a98a"
                            :device           "foobar"
                            :plan             "baremetal_0"
                            :facility         "ewr1"
                            :operating_system "ubuntu_16_04"}
                   id      (:id data)
                   opts    (dissoc data :id)
                   m       (packet/bootstrap-packet-cljs)
                   request (<! (martian/response-for m :create-device {:id            id
                                                                       ::martian/body opts}))]
               (is (= []
                      request)))
             (done))))
