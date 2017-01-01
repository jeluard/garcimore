(ns garcimore.jdbc-tests
  (:require [clojure.java.jdbc :as j]
            [clojure.test :refer [deftest are is]]
            [garcimore.jdbc :as gj]
            [garcimore.liquibase :as gl])
  (:import [org.h2.jdbcx JdbcDataSource]))

(def insert-multi! j/insert-multi!)

(defn datasource
  []
  (let [ds (JdbcDataSource.)]
    (.setURL ds "jdbc:h2:mem:test2")
    (.setUser ds "sa")
    (.setPassword ds "sa")
    ds))

(deftest schema
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (gj/execute! conn ["CREATE TABLE FRUIT (ID INT PRIMARY KEY, appearance VARCHAR(100), cost INT)"])

    (is (= {:columns     {"APPEARANCE" {:autoincrement? false
                                        :column-size    100
                                        :decimal        0
                                        :default        nil
                                        :nullable?      true
                                        :radix          10
                                        :type           :varchar}
                          "ID"         {:autoincrement? false
                                        :column-size    10
                                        :decimal        0
                                        :default        nil
                                        :nullable?      false
                                        :radix          10
                                        :type           :integer}
                          "COST"       {:autoincrement? false
                                        :column-size    10
                                        :decimal        0
                                        :default        nil
                                        :nullable?      true
                                        :radix          10
                                        :type           :integer}}
            :constraints {:primary-keys ["ID"]}}
           (gj/schema (.getMetaData conn) "FRUIT")))

    (is (= nil (gj/schema (.getMetaData conn) "NON-EXISTENT")))))

(deftest schemas
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (gj/execute! conn ["CREATE TABLE FRUIT (ID INT PRIMARY KEY, appearance VARCHAR(100), cost INT)"])

    (is (= {"FRUIT" {:columns     {"APPEARANCE" {:autoincrement? false
                                                 :column-size    100
                                                 :decimal        0
                                                 :default        nil
                                                 :nullable?      true
                                                 :radix          10
                                                 :type           :varchar}
                                   "ID"         {:autoincrement? false
                                                 :column-size    10
                                                 :decimal        0
                                                 :default        nil
                                                 :nullable?      false
                                                 :radix          10
                                                 :type           :integer}
                                   "COST"       {:autoincrement? false
                                                 :column-size    10
                                                 :decimal        0
                                                 :default        nil
                                                 :nullable?      true
                                                 :radix          10
                                                 :type           :integer}}
                     :constraints {:primary-keys ["ID"]}}}
           (gj/schemas conn)))))

(deftest data
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (is (= {} (gj/data conn)))

    (gj/execute! conn ["CREATE TABLE FRUIT (ID INT PRIMARY KEY, appearance VARCHAR(100), cost INT)"])

    (is (= {"FRUIT" []} (gj/data conn)))

    (insert-multi! {:connection conn} :fruit
                   [{:id 1 :appearance "rosy" :cost 24}])

    (is (= {"FRUIT" [{"ID" 1 "APPEARANCE" "rosy" "COST" 24}]} (gj/data conn)))))
