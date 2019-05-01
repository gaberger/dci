(ns lib.packet-test
  (:require [lib.packet :as packet]
            [dci.state :refer [app-state]]
            [utils.core :as utils]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [cljs.test :refer-macros [deftest testing is run-tests async]]))

(assert (not (nil? (utils/get-env "APIKEY"))))
(assert (not (nil? (utils/get-env "ORGANIZATION_ID"))))
(swap! app-state assoc :debug true)

(def test-state (atom {}))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (println "Success!")
    (println "FAIL")))


#_(deftest create-test
  (let [project (packet/create-project {:name "test-project"})]
    (async done
         (go
           (let [project-id (:id (:body (<! project)))
                 device     (:id (:body (<! (packet/create-device {:id         project-id
                                                       :name       "test-device"
                                                       :plan       "baremetal_0"
                                                       :facilities ["ewr1"]
                                                       :os         "ubuntu_16_04"}))))
                 result (<! (packet/get-device device))]
             (swap! test-state assoc :project-id project)
             (swap! test-state assoc :device-id device)
               (is (= 200
                      (:status result)))
         (done))))))


(deftest project-create-delete
  (let [project-name "test"]
    (async done
         (go
           (if-not (<! (packet/project-exist? project-name))
             (let [project (:id (:body (<! (packet/create-project {:name project-name}))))]
                                        ;(<! (timeout 60000))
               (is (true? (<! (packet/project-exist? "test"))))
               (is (= 204 (:status (<! (packet/delete-project project)))))
               (done)))))))

#_(deftest organization-test
  (async done
         (go
           (let [result  (<! (packet/list-organizations))]
             (is (= 200 (:status result)))
             (done)))))


#_(deftest delete-test
 (async done
           (go
             (is (= 200 (:status (<! (packet/delete-device (:device-id @test-state))))))
             (<! (timeout 60000)) 
             (is (= 200 (:status     (<! (packet/delete-project (:project-id @test-state)))
             (done)))))))

