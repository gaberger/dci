(ns utils.command
  (:require [ssh2shell]
            [promesa.core :as p]
            [promesa.async-cljs :refer-macros [async]]
            [clojure.string :as str]))


#_(p/alet
 (-> (p/alet [a  (p/await (asyncFunction1 "foo"))
              b  (p/await (asyncFunction2 "bar" a))]
             do something ...)
     (catch (fn [error] 
              handle errors ... ))))

;;TODO spec to fullfill map options
;;should we add defaults?
(defn create-service
  """
  Create map defining service definition.
   host is computed as a monotonically incrementing post-fix on service-name
   optional keys [:location :plan :os]
  (create-service "etcd" 5 {:plan "plan" :location "location" :os "os"})
  {:etcd [{:location location, :plan plan, :os os, :name etcd-1} {:location location, :plan plan, :os os, :name etcd-2} {:location   location, :plan plan, :os os, :name etcd-3} {:location location, :plan plan, :os os, :name etcd-4}
  {:location location, :plan plan, :os os, :name etcd-5}]}
  """
  [service-name count & args]
  (let [acc  (atom [])
        keys (apply merge (for [[k v] (select-keys (first args) [:name :location :plan :os])]
                            {k v}))]
    (dotimes [x count]
      (let [name (str/join "-" [service-name (inc x)])
            m    (apply merge (list keys {:name name}))]
        (swap! acc conj m)))
    (assoc {} (keyword service-name) @acc)
    )
  )




