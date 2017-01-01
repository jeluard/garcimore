(ns garcimore.h2-tests
  (:require [clojure.java.jdbc :as j]
            [clojure.test :refer [deftest are is]]
            [garcimore.dml :as gdml]
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

(deftest data
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (is (= {} (gj/data conn)))

    (gj/execute! conn ["CREATE TABLE FRUIT (name VARCHAR(100) PRIMARY KEY, appearance VARCHAR(100), cost INT)"])

    (is (= {"FRUIT" []} (gj/data conn)))

    (insert-multi! {:connection conn} :fruit
                   [{:name "Apple" :appearance "rosy" :cost 24}])

    (is (= {"FRUIT" [{"NAME" "Apple" "APPEARANCE" "rosy" "COST" 24}]} (gj/data conn)))))

(deftest diff-table
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (let [m (gj/data conn)]
      (gj/execute! conn ["CREATE TABLE FRUIT (name VARCHAR(100) PRIMARY KEY, appearance VARCHAR(100), cost INT)"])
      (insert-multi! {:connection conn} :fruit
                     [{:name "Apple" :appearance "rosy" :cost 24}
                      {:name "Orange" :appearance "round" :cost 49}])
      (is (= {"FRUIT" {:inserted [{"APPEARANCE" "round"
                                   "COST"       49
                                   "NAME"       "Orange"}
                                  {"APPEARANCE" "rosy"
                                   "COST"       24
                                   "NAME"       "Apple"}]}}
             (gdml/diff-tables (gj/schemas conn) m (gj/data conn)))))))

(deftest roundtrip
  (with-open [conn (.getConnection (datasource) "root" "root")]
    (let [m1 {"FRUIT"
              [{"APPEARANCE" "round"
                "COST"       49
                "NAME"       "Orange"}
               {"APPEARANCE" "sdf"
                "COST"       49
                "NAME"       "Pear"}]}]
      (gj/execute! conn ["CREATE TABLE FRUIT (name VARCHAR(100) PRIMARY KEY, appearance VARCHAR(100), cost INT)"])
      (let [ms (gj/schemas conn)
            diff (gdml/diff-tables ms (gj/data conn) m1)]
        (gj/execute! conn (gl/diff->sql ms diff :h2)))
      (is (= m1 (gj/data conn))))))
