(ns dci.utils.macros
  (:require [kitchen-async.promise :as p :require-macros [let loop recur]]))

(defmacro dotimes*
    [bindings & body]
    (p/let [i (first bindings)
            n (second bindings)]
      `(p/let [n# ~n]
         (p/loop [~i 0]
           (when (< ~i n#)
             ~@body
             (p/recur (inc ~i)))))))


(def exports #js {})
