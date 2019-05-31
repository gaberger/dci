(ns test.server.server-test
 (:require [dci.drivers.packet :as packet]
          [dci.state :refer [app-state]]
          [dci.utils.core :as utils]
          [dci.drivers.packet :as api]
          [w3c-xmlhttprequest-plus :refer [XMLHttpRequest]]
          [schema.core :as s]
          [martian.core :as martian]
          [martian.test :as martian-test]
          [cljs.test :refer-macros [deftest testing is run-tests async]]
          [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]))

(assert (not (nil? (utils/get-env "APIKEY"))))
(assert (not (nil? (utils/get-env "ORGANIZATION_ID"))))
(set! js/XMLHttpRequest XMLHttpRequest)
(swap! app-state assoc :debug true)

 (deftest organization-test
   (async done
       (go
         (let [m (-> (api/bootstrap-packet-cljs)
                     #_(martian-test/respond-as :cljs-http)
                     #_(martian-test/respond-with-generated {:get-organizations :success})
                     (martian-test/respond-with-constant {:status 200}))]
           (is (= []
                  (<! (martian/response-for m :get-organizations))
                  ))

           #_(is (= 200 (:status (<! (martian/response-for m :get-organizations)))))
           (done)))))

 
   ;; => ExceptionInfo Value cannot be coerced to match schema: {:id missing-required-key}

   #_(martian/response-for m :get-organization {:id "bad-id"})
   ;; => ExceptionInfo Value cannot be coerced to match schema: {:id (not (integer? bad-id))}

   #_(martian/response-for m :get-organization {:id 123})
 ;; => {:status 200, :body {:id -3, :name "EcLR"}}
          ;[server.dci-model :as model :refer [IServer]]
 



