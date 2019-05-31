(ns dci.utils.remote
  (:require [ssh2shell :as SSH2Shell]
            [clojure.string :as str]
            [cljs.core.async :refer [<! >! timeout take! chan] :refer-macros [go go-loop]]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]))


(defn host-factory [host port user password]
  {:server
   {:host             host
    :verbose          false
    :hashKey          "1234"
    :port             port
    :userName         user
    :password         password
    :connectedMessage nil
    :readyMessage     nil
    :closedMessage    nil 
    }})

(defn create-shell [host]
  (SSH2Shell. host))

(defn command-handler [session-text]
  (debug session-text))

(defn call-ssh [response-chan host]
  (let [SSH (create-shell host)]
      (.connect SSH  #_(fn [session-text]
                         (command-handler session-text)))
      (.on SSH "error" (fn [e t _ _]
                         (error t)))
      (.on SSH "connect" (fn [msg]
                           (info msg)))
      (.on SSH "msg" (fn [msg]
                       (info msg)))
      (.on SSH "commandComplete"  (fn [command response sshobj]
                                    (go
                                      (>! response-chan response))))
      (.on SSH "end" (fn [session-text, sshobj]
                       (debug session-text)))))

(def exports #js {})




