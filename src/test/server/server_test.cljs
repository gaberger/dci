(ns test.server.server-test
 (:require [dci.drivers.packet :as packet]
          [dci.state :refer [app-state]]
          [dci.utils.core :as utils]
          [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
          [cljs.test :refer-macros [deftest testing is run-tests async]]
          ;[server.dci-model :as model :refer [IServer]]
          ))

(assert (not (nil? (utils/get-env "APIKEY"))))
(assert (not (nil? (utils/get-env "ORGANIZATION_ID"))))
(swap! app-state assoc :debug false)

#_(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (println "Success!")
    (println "FAIL")))

#_(deftest get-devices-organization-test
  (async done
         (go
           (is (= {} (<! (model/get-devices-organization (PacketServer.)
                                                   {:id (utils/get-env "ORGANIZATION_ID") :options nil}))))
           (done))))
