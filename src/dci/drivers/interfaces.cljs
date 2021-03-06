(ns dci.drivers.interfaces)

(defmulti get-facilities "Get Facilties Resource" (fn [driver] driver :default :packet))
(defmulti print-facilities "Print Facilties Resource" (fn [driver] driver :default :packet))
(defmulti get-organizations "Get Organizations Resource" {:arglists '([driver])}
  (fn [driver] driver) :default :packet)
(defmulti print-organizations "Print Organizations Resource" {:arglists '([driver])}
  (fn [driver  & options] driver) :default :packet)

(defmulti get-projects "Get Projects Resource" {:arglists '([driver organizaton-id options])}
  (fn [driver organization-id & options] driver))
(defmulti print-projects "Print Projects Resource" {:arglists '([driver organization-id options])}
  (fn [driver organization-id & options] driver))

(defmulti get-devices-organization "Get Devices by Organization" {:arglists '([driver organizaton-id options])}
  (fn [driver organization-id & options] driver))
(defmulti print-devices-organization "Print Devices by Organization" {:arglists '([driver organization-id options])}
  (fn [driver organization-id & options] driver))

(defmulti print-devices-project "Print Devices by Project" (fn [driver project-id & options] driver))
(defmulti get-devices-project "Get Devices by Project" (fn [driver project-id & options] driver))

(defmulti get-device "Get Device Resource" {:arglists '([driver organization-id device-id])} (fn [driver organization-id device-id & options] driver))
(defmulti print-device "Print Device Rsource" {:arglists '([driver device-id])} (fn [driver device-id & options] driver))

(defmulti get-project "Get Project Resource" {:arglists '([driver project-id])} (fn [driver project-id & options] driver))
(defmulti print-project "Print Project Resource" {:arglists '([driver organization-id])} (fn [driver organization-id & options] driver))
(defmulti get-project-name "Get Project Name From ID" {:arglists '([driver project-id])} (fn [driver project-id & options] driver))
(defmulti get-projectid-prefix "Get Project ID From Prefix" {:arglists '([driver organization-id prefix])} (fn [driver organization-id prefix] driver))
(defmulti get-project-id "Get Project ID From Name" {:arglists '([driver organizaton-id project-name])} (fn [driver organization-id project-name & options] driver))

(defmulti project-exist? "Does Project Exist?" (fn [driver id-or-name & options] [driver id-or-name]))

(defmulti create-project "Create Project"  {:arglists '([driver organization-id project-name options])}
  (fn [driver organization-id project-name & options] driver))
(defmulti delete-project "Delete Project"  {:arglists '([driver project-id options])} (fn [driver project-id & options] driver))

(defmulti device-exist? "Does Device Exist?" (fn [driver project-or-organization project-or-organization name] [driver project-or-organization project-or-organization name]))
(defmulti create-device "Create Device" (fn [driver project-id & options] driver))
(defmulti create-device-batch "Create Device Batch" (fn [driver project-id & options] driver))
(defmulti delete-device "Delete Device" (fn [driver project-id device-id & options] driver))
(defmulti get-device-events "Get Device Event Log" (fn [driver device-id & options] driver))
(defmulti print-device-events "Print Device Event Log" (fn [driver device-id & options] driver))
(defmulti get-deviceid-prefix "Get Device ID From Prefix" {:arglists '([driver project-id prefix])} (fn [driver project-id prefix] driver))
(defmulti gen-inventory "Generate Ansible Inventory " {:arglists `([driver organization-id project-id])} (fn [driver organization-id project-name] driver))
  (defmulti get-ssh-keys "Get ssh public keys" {:arglists `([driver])} (fn [driver] driver))


(def exports #js {})
