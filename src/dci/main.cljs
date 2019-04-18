(ns dci.main
  (:require [commander]
            [utils.core :as utils]
            [clojure.string :as str]))

(defn command-handler []
 (.. commander
     (version "0.1.0")
     (command "server <command>" "Server operations")
     (description "Dell \"Bare Metal Cloud\" Command Interface")
     (parse (.-argv js/process))))

(defn main! []
  (when-not (utils/state-exists)
    (utils/initialize-state))
   (command-handler))

