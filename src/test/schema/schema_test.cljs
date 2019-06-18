(ns test.schema.schema-test
  (:require    [cljs.test :refer-macros [deftest testing is run-tests async]]
               [schema.core :as s :include-macros true]
               [dci.utils.core :as utils]))

(deftest specs-bad-test
  (async done
         (let [cluster-file (utils/read-cluster-file "./src/test/schema/test-bad.yaml")]
           (is (thrown? js/Error (s/validate dci.schemas.core/cluster-spec cluster-file))))
         (done)))

(deftest specs-good-test
  (async done
         (let [cluster-file (utils/read-cluster-file "./src/test/schema/test-good.yaml")]
           (is []
                (s/validate dci.schemas.core/cluster-spec cluster-file)))
         (done)))

(deftest validator-test-good
  (async done
         (let [cluster-file (utils/read-cluster-file "./src/test/schema/test-good.yaml")
               err (utils/valid-cluster-spec cluster-file)]
           (is (= err false)))
           (done)))

(deftest validator-test-bad
  (async done
         (let [cluster-file (utils/read-cluster-file "./src/test/schema/test-bad.yaml")
               err (utils/valid-cluster-spec cluster-file)]
           (is (= err true)))
         (done)))

(deftest validator-multi-node-spec
  (async done
         (let [cluster-file (utils/read-cluster-file "./src/test/schema/test-good-mult.yaml")
               err (utils/valid-cluster-spec cluster-file)]
           (is (= err false)))
         (done)))