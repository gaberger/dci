(ns dci.utils.command
  (:require [cljs-node-io.proc :refer [spawn-sync spawn exec aexec]]
            [cljs-node-io.core :refer [stream-type]]
            [cljs-node-io.async :refer [go-proc server->ch]]
            [oops.core :refer [oget oset!]]
            [kitchen-async.promise :as p]
            [kitchen-async.promise.from-channel]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [super-progress :refer [Progress]]
            [dci.state :refer [app-state]]
            [clojure.string :as str]))

(defn tick-loop [progress]
  (let [tick-state (atom 0)
        interval (atom nil)]
    (reset! interval (js/setInterval (fn []
                                       (swap! tick-state inc)
                                       (.tick progress)
                                       (if (>= @tick-state 100)
                                           (js/clearInterval @interval))) 100))
    ))

(defn run-progress-command [command]
  (p/let [progress (.create Progress -1)]
    (oset! (.-options progress) :pattern "{spinner}")
    (tick-loop progress)
      (when-let [reader (aexec command)]
        (if-some [error-msg (first reader)]
          (error error-msg)
          (fnext reader)))
      (.complete progress)
      ))

(defn run-command [command args]
  (when (:debug @app-state) (debug "Running Command: " command))
     (try
       (when-let [reader (spawn command args {})]
         (.on reader "end" (fn []
                             (info "Got end")))
         (.on (.-stdout reader) "data" (fn [data]
                                         (info (str/trim (.toString data)))))
         (.on (.-stderr reader) "data" (fn [data]
                                        (info (str/trim (.toString data))))))
       (catch js/Error e
         (error "Caught error in command" e)
         (.exit js/process 1))
       ))

(def exports #js {})

