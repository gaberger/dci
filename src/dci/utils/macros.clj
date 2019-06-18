#_(ns dci.utils.macros
  (:require [kitchen-async.promise :as p :require-macros [let loop recur]]
            ))

#_(defmacro dotimes*
    [bindings & body]
    (p/let [i (first bindings)
            n (second bindings)]
      `(p/let [n# ~n]
         (p/loop [~i 0]
           (when (< ~i n#)
             ~@body
             (p/recur (inc ~i)))))))


#_[{:options ["-D --debug" "Debug"]
  :version "0.0.3"}]

#_(defmacro command
  [version description command-str bindings & body]
  (let [i (first bindings)
        n (second bindings)
        program ]

    `(.. program)

    )




  )




(def exports #js {})
