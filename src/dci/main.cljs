(ns dci.main
  (:require [commander]
            [clojure.string :as str]))

(def app-state (atom {:debug false}))

(defn command-handler []
 (.. commander
     (version "0.1.0")
     (command "server <command>" "Server operations")
     (description "Dell \"Bare Metal Cloud\" Command Interface")
     (parse (.-argv js/process))))

(defn main! []
   (command-handler))

