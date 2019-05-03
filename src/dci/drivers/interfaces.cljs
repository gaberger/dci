(ns dci.drivers.interfaces)
(defmulti g "Better function" {:arglists '([x])} (fn [x] :blah))

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

(defmulti get-devices-project "Get Devices by Project" (fn [driver project-id & options] driver))
(defmulti print-devices-project "Print Devices by Project" (fn [driver project-id & options] driver))

(defmulti get-device "Get Device Resource" {:arglists '([driver device-id])} (fn [driver device-id & options] driver))
(defmulti print-device "Print Device Rsource" {:arglists '([driver device-id])} (fn [driver device-id & options] driver))

(defmulti get-project "Get Project Resource" {:arglists '([driver organization-id])} (fn [driver organization-id & options] driver))
(defmulti print-project "Print Project Resource" {:arglists '([driver organization-id])} (fn [driver organization-id & options] driver))
(defmulti get-project-name "Get Project Name From ID" (fn [driver & options] driver))

(defmulti project-exist? "Does Project Exist?" (fn [driver & options] driver))

(defmulti create-project "Create Project"  {:arglists '([driver organization-id options])}
  (fn [driver organization-id & options] driver))
(defmulti delete-project "Delete Project"  {:arglists '([driver project-id options])} (fn [driver project-id & options] driver))

(defmulti device-exist? "Does Device Exist?" (fn [driver project-or-organization & options] [driver project-or-organization]))
(defmulti create-device "Create Device" (fn [driver project-id & options] driver))
(defmulti delete-device "Delete Device" (fn [driver device-id & options] driver))

(def exports #js {})
