(ns garcimore.liquibase-tests
  (:require [clojure.test :refer [deftest are is]]
            [garcimore.liquibase :as gl]))

(deftest generate
  (is (= (list "INSERT INTO configuration (scheme_id, `key`, value) VALUES ('1', 'k', 'v')")
         (gl/generate
           [(gl/insert-statements "configuration" {} {"scheme_id" "1" "key" "k" "value" "v"})]
           :mariadb))))

(deftest diff->sql
  (is (= (list "INSERT INTO FRUIT (APPEARANCE, COST, ID) VALUES ('round', 49, 1)")
         (gl/diff->sql
           {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
           {"FRUIT" {:inserted [{"APPEARANCE" "round"
                                 "COST"       49
                                 "ID"         1}]}}
           :mariadb)))
  (is (= (list "UPDATE FRUIT SET COST = 50 WHERE ID = 1")
         (gl/diff->sql
           {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
           {"FRUIT" {:updated [{"COST"       [49 50]
                                "ID"       1}]}}
           :mariadb)))
  (is (= (list "DELETE FROM FRUIT WHERE ID = 1")
         (gl/diff->sql
           {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
           {"FRUIT" {:deleted [{"ID"       1}]}}
           :mariadb)))
  (is (= (list "INSERT INTO FRUIT (APPEARANCE, COST, ID) VALUES ('round', 49, 1)"
               "INSERT INTO FRUIT (APPEARANCE, COST, ID) VALUES ('rosy', 24, 2)")
         (gl/diff->sql
           {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
           {"FRUIT" {:inserted [{"APPEARANCE" "round"
                                 "COST"       49
                                 "ID"       1}
                                {"APPEARANCE" "rosy"
                                 "COST"       24
                                 "ID"         2}]}}
           :mariadb))))
