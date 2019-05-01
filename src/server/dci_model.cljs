(ns server.dci-model)

(defprotocol IServer
  (get-organizations [this])
  (print-organizations [this])

  (get-projects [this])
  (print-projects [this])

  (get-server [this id])
  (list-plans [this])
  (get-project-name [this project-id])
  (list-servers [this project-id] [this project-id options])
  (create-server [this  args])
  (create-service [this args])
  (create-project [this  args])
  (delete-project [this  args])
  (delete-server [this device-id])
  (start-server [this device-id])
  (stop-server [this device-id]))
