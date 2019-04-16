(ns dci.build
  (:require
   [shadow.cljs.devtools.api :as shadow]
   [clojure.java.shell :refer (sh)]))


(defn compile []
  (let [build-ids (into [] (shadow/get-build-ids))
        final-ids (into [] (remove #{:npm :test} build-ids))]
    (doall
     (map (fn [module]
            (shadow/compile module)) final-ids))))

(defn release []
  (let [build-ids (into [] (shadow/get-build-ids))
        final-ids (into [] (remove #{:npm :test} build-ids))]
    (doall
     (map (fn [module]
           (shadow/release module)) final-ids))))

(defn native []
  (release)
      (sh "pkg" "package.json" "--output" "bin/dci"))
