(ns utils.core)

(defn print-json [obj]
  (println (.stringify js/JSON (clj->js obj) nil " ")))
