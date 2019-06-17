(ns test.schema.core
  (:require [dci.utils.core :as utils]
            [cljs.test :refer-macros [deftest testing is run-tests async]]))



(deftest schema-test
  (let [spec (utils/read-cluster-file "../specs/kube-cluster.yaml")]


