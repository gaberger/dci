(ns dci.build
  (:require
   [shadow.cljs.devtools.api :as shadow]
   [me.raynes.fs :as fs]
   [me.raynes.fs.compression :as compress]
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

(defn archive []
  (native)
  (let [files (mapv #(fs/normalized %) (fs/list-dir "./bin"))]
      (doall
       (map (fn [file]
              (println (.toString file))
              (sh "gzip" (.toString file))) files))))


