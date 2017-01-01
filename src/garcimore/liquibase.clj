(ns garcimore.liquibase
  (:require [clojure.spec :as spec])
  (:import [java.util Date]
           [liquibase.change Change ColumnConfig]
           [liquibase.change.core AbstractModifyDataChange DeleteDataChange InsertDataChange UpdateDataChange]
           [liquibase.database Database]
           [liquibase.database.core DB2Database DerbyDatabase FirebirdDatabase H2Database InformixDatabase MariaDBDatabase MySQLDatabase OracleDatabase PostgresDatabase SQLiteDatabase SybaseASADatabase SybaseDatabase H2Database MariaDBDatabase]
           [liquibase.sql Sql]
           [liquibase.sqlgenerator SqlGeneratorFactory]))

(def dbs
  {:db2       (DB2Database.)
   :derby     (DerbyDatabase.)
   :firebird  (FirebirdDatabase.)
   :h2        (H2Database.)
   :informix  (InformixDatabase.)
   :mariadb   (MariaDBDatabase.)
   :mysql     (MySQLDatabase.)
   :oracle    (OracleDatabase.)
   :postgres  (PostgresDatabase.)
   :sqlite    (SQLiteDatabase.)
   :sybaseas  (SybaseASADatabase.)
   :sybase    (SybaseDatabase.)})

(defmulti set-column-value (fn [_ o] (type o)))

(defmethod set-column-value String [^ColumnConfig c ^String s] (.setValue c s))
(defmethod set-column-value Long [^ColumnConfig c ^Long n] (.setValueNumeric c n))
(defmethod set-column-value Double [^ColumnConfig c ^Double n] (.setValueNumeric c n))
(defmethod set-column-value Boolean [^ColumnConfig c ^Boolean b] (.setValueBoolean c b))
(defmethod set-column-value Date [^ColumnConfig c ^Date d] (.setValueDate c d))
(defmethod set-column-value :default [_ v] (println "unknown? " v) false)

(defn column
  [ms k v]
  (let [c (ColumnConfig.)]
    (.setName c k)
    (set-column-value c v)
    c))
int?
(defn columns
  [ms m]
  (map #(column ms (key %) (val %)) m))

(defn insert-statements
  [t ms m]
  (doto (InsertDataChange.)
    (.setTableName t)
    (.setColumns (columns ms m))))

(defn set-where-params!
  [^AbstractModifyDataChange ch ms m]
  (doseq [c (columns ms m)]
    (.setWhere ch ":name = :value")
    (.addWhereParam ch c)))

(defn pk-columns
  [ms m]
  (let [ks (:primary-keys ms)]
    (reduce-kv (fn [m k v] (if (some #(= k %) ks) (assoc m k v) (dissoc m k))) {} m)))

(defn updated-columns
  [ms m]
  ;; TODO make sure all values are vector
  (let [ks (:primary-keys ms)]
    (reduce-kv (fn [m k v] (if (some #(not= k %) ks) (assoc m k (second v)) (dissoc m k))) {} m)))

(defn update-statements
  [t ms m]
  (let [ch (UpdateDataChange.)]
    (.setTableName ch t)
    (.setColumns ch (columns ms (updated-columns ms m)))
    (set-where-params! ch ms (pk-columns ms m))
    ch))

(defn delete-statements
  [t ms m]
  (let [ch (DeleteDataChange.)]
    (.setTableName ch t)
    (set-where-params! ch ms (pk-columns ms m))
    ch))

(defn create-table-statements
  [t ms m]
  (let [ch (DeleteDataChange.)]
    (.setTableName ch t)
    (set-where-params! ch ms (pk-columns ms m))
    ch))

(spec/def ::change #(instance? Change %))

(spec/fdef generate
           :args (spec/+ ::change)
           :ret (spec/+ string?))

(defn generate
  "Generate SQL as a seq of Strings from a seq of liquibase.change.Change"
  [s db]
  (let [^SqlGeneratorFactory g (SqlGeneratorFactory/getInstance)]
    (flatten
      (for [^Change c s]
        (for [^Sql sql (.generateSql g c ^Database (db dbs))]
          (.toSql sql))))))

(defn diff->sql
  [ms m k]
  (generate
    (flatten
      (for [[k v] m
            :let [mm (get-in ms [k :constraints])]]
        (concat
          (when-let [s (:inserted v)]
            (map #(insert-statements k mm %) s))
          (when-let [s (:updated v)]
            (map #(update-statements k mm %) s))
          (when-let [s (:deleted v)]
            (map #(delete-statements k mm %) s)))))
    k))
