(ns server.dci-model)

(defprotocol IServer
  (list-projects [this])
  (get-project-name [this project-id])
  (list-servers [this project-id] [this project-id options])
  (create-server [this  args])
  (delete-server [this device-id])
  (start-server [this device-id])
  (stop-server [this device-id]))
