(ns test.lib.packet-test
  (:require [dci.drivers.interfaces :as api]
            [dci.drivers.packet]
            [dci.state :refer [app-state]]
            [dci.utils.core :as utils]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [cljs.test :refer-macros [deftest testing is run-tests async]]))

(assert (not (nil? (utils/get-env "APIKEY"))))
(assert (not (nil? (utils/get-env "ORGANIZATION_ID"))))
(assert (not (nil? (utils/get-env "PROJECT_ID"))))
(swap! app-state assoc :debug false)

(def test-state (atom {}))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (println "Success!")
    (println "FAIL")))

#_(deftest get-organizations-test
  (async done
         (go
           (let [result (<! (api/get-organizations :packet ))]
             (println result)
           (is (= 200
                  (:status result))))
           (done))))

#_(deftest get-projects-test
  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
    (async done
         (go
           (let [result (<! (api/get-projects :packet organization-id))]
             (is (= 200
                    (:status result))))
           (done)))))

#_(deftest get-devices-organization-test
  (let [organization-id (utils/get-env "ORGANIZATION_ID")]
    (async done
           (go
             (let [result (<! (api/get-devices-organization :packet organization-id))]
               (is (= 200
                      (:status result))))
             (done)))))

#_(deftest get-devices-project-test
  (let [project-id (utils/get-env "PROJECT_ID")]
    (async done
           (go
             (let [result (<! (api/get-devices-project :packet project-id))]
               (is (= 200
                      (:status result))))
             (done)))))


#_(deftest get-create-project-test
  (let [organization-id (utils/get-env "ORGANIZATION_ID")
        project-name    "test"]
    (async done
           (p/let [exists? (api/project-exist? :packet {:organization-id organization-id
                                                           :name            project-name})]
             (if-not exists?
               (p/let [result1 (api/create-project :packet organization-id {:name        project-name})
                       project-id (-> result1 :body :id)
                       result2 (api/delete-project :packet project-id)]
                 (is (= 201 (:status result1)))
                 (is (= 204 (:status result2))))
               (is (false? exists?)))
               (done)))))

#_(deftest project-create-delete-test
  (let [organization-id (utils/get-env "ORGANIZATION_ID")
        project-name    "test"]
  (async done
         (p/let [exists? (packet/project-exist? :packet {:organization-id organization-id
                                                         :name            project-name})]
           (if-not exists?
             (p/let [project  (packet/create-project :packet {:organization-id organization-id
                                                              :name            project-name})
                     project-id (-> project :body :id)
                     device (packet/create-device :packet {:project-id project-id
                                                           :name       "test-device"
                                                           :plan       "baremetal_0"
                                                           :facility   ["ewr1"]
                                                           :os         "ubuntu_16_04"})
                     device-id (-> device :body :id)
                     _ (timeout 70000) ;;delay to give time for server to be provisioned
                     ]

               (is (= 201 (:status device)))
               (p/let [device   (packet/delete-device :packet {:device-id device-id})
                       project  (packet/delete-project :packet {:project-id project-id})]
                 (is (= 204 (:status device)))
                 (is (= 204 (:status project)))))
             (is (false? exists?)))
                     (done)))))


