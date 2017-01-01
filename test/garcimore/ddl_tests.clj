(ns garcimore.ddl-tests
  (:require [clojure.test :refer [deftest are is]]
            [garcimore.ddl :as gddl]))

(deftest primary-key-values
  (is (= {"id" 1} (gddl/primary-key-values ["id"] {"id" 1 "key""value"})))
  (is (= {"id" 1 "key" "value"} (gddl/primary-key-values ["id" "key"] {"id" 1 "key""value"}))))


(deftest keys-values-map
  (is (= {{"id" 1} {"id"  1
                    "key" "value"}}
         (gddl/keys-values-map ["id"] [{"id" 1 "key""value"}]))))

(deftest diff-table-schema
  ; add a column
  (is (= {:inserted {:columns {"COLUMN" {:type :varchar}}}}
         (gddl/diff-table-schema {}
                                 {:columns {"COLUMN" {:type :varchar}}})))

  ; remove a column
  (is (= {:deleted {:columns {"COLUMN" {:type :varchar}}}}
         (gddl/diff-table-schema {:columns {"COLUMN" {:type :varchar}}}
                                 {})))

  ; rename a column
  (is (= {:columns {"COLUMN1" {:to {:name "COLUMN2"}}}}
         (gddl/diff-table-schema {:columns {"COLUMN1" {:type :varchar}}}
                                 {:columns {"COLUMN2" {:type :varchar}} :from "COLUMN1"})))

  ; alter a column
  (is (= {:columns {"COLUMN" {:from {:type :varchar} :to {:type :boolean}}}}
         (gddl/diff-table-schema {:columns {"COLUMN" {:type :varchar}}}
                                 {:columns {"COLUMN" {:type :boolean}}})))

  ; alter a constraints
  (is (= {:constraints {:to {:primary-keys ["COLUMN"]}}}
         (gddl/diff-table-schema {:columns {"COLUMN" {:type :varchar}}}
                                 {:columns {"COLUMN" {:type :varchar}}
                                  :constraints {:primary-keys ["COLUMN"]}})))


  (is (= {:columns {"COLUMN1" {:from {:type :varchar}}
                    "COLUMN2" {:to {:type :boolean}}
                    "COLUMN3" {:to {:type :varchar :nullable? false}}}
          :constraints {:to {:primary-keys ["COLUMN2" "COLUMN3"]}}}
         (gddl/diff-table-schema {:columns {"COLUMN1" {:type :varchar}}}
                                 {:columns {"COLUMN3" {:type :varchar :nullable? false}
                                            "COLUMN2" {:type :boolean}} :from "COLUMN1"
                                  :constraints {:primary-keys ["COLUMN2" "COLUMN3"]}}))))

#_
(deftest merge-table-schema
  (is (= {:columns {"COLUMN" {:type :varchar}}}
         (gddl/merge-table-schema [{:inserted {:columns {"COLUMN" {:type :varchar}}}}]))))

(deftest diff-schema
  ; add a table
  (is (= {"TABLE" {:to {:columns {"COLUMN" {:type :varchar}}}}}
         (gddl/diff-schema {}
                           {"TABLE" {:columns {"COLUMN" {:type :varchar}}}})))

  ; remove a table
  (is (= {"TABLE" {:from {:columns {"COLUMN" {:type :varchar}}}}}
         (gddl/diff-schema {"TABLE" {:columns {"COLUMN" {:type :varchar}}}}
                           {})))

  ; rename a table
  (is (= {"TABLE1" {:to {:name "TABLE2" :columns {"COLUMN" {:type :varchar}}}}}
         (gddl/diff-schema {"TABLE1" {:columns {"COLUMN" {:type :varchar}}}}
                           {"TABLE2" {:columns {"COLUMN" {:type :varchar}} :from "TABLE1"}})))

  ; alter a table
  (is (= {"TABLE1" {:from {:columns {"COLUMN1" {:type :varchar}}} :to {:columns {"COLUMN2" {:type :varchar}}}}}
         (gddl/diff-schema {"TABLE" {:columns {"COLUMN1" {:type :varchar}}}}
                           {"TABLE" {:columns {"COLUMN2" {:type :varchar}}}}))))
