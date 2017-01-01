(ns garcimore.jdbc
  (:require [clojure.spec :as spec])
  (:import [java.sql Connection DatabaseMetaData ResultSet JDBCType]))

; use setReadOnly when not doing changes for safety and perf
; catalog abstraction!
; getWarnings / clearWarnings to ensure there was no error
; Savepoints

; https://github.com/jOOQ/jOOQ/blob/master/jOOQ-meta-extensions/src/main/java/org/jooq/util/jpa/JPADatabase.java

(spec/def ::connection #(instance? Connection %))

(spec/fdef execute!
  :args (spec/cat :conn ::connection)
  :ret nil)

(defn execute!
  [^Connection conn s]
  (doseq [sql s]
    (.executeUpdate (.createStatement conn) sql)))

(defn raw-resultset-seq
  "Creates and returns a lazy sequence of structmaps corresponding to
  the rows in the java.sql.ResultSet rs"
  {:added "1.0"}
  [^ResultSet rs]
  (let [rsmeta (. rs (getMetaData))
        idxs (range 1 (inc (. rsmeta (getColumnCount))))
        keys (map (fn [i] (. rsmeta (getColumnLabel i))) idxs)
        check-keys
        (or (apply distinct? keys)
            (throw (Exception. "ResultSet must have unique column labels")))
        row-struct (apply create-struct keys)
        row-values (fn [] (map (fn [^Integer i] (. rs (getObject i))) idxs))
        rows (fn thisfn []
               (when (. rs (next))
                 (cons (apply struct row-struct (row-values)) (lazy-seq (thisfn)))))]
    (rows)))

(defn nullable?
  [m]
  (condp = (:nullable m)
    DatabaseMetaData/columnNoNulls false
    DatabaseMetaData/columnNullable true
    nil))

(defn jdbc-type->type
  [t]
  (keyword (.toLowerCase (.name t))))

(defn column
  [m]
  {(:column_name m) {:type (jdbc-type->type (JDBCType/valueOf (:data_type m))) :column-size (:column_size m) :nullable? (nullable? m)
                     :decimal (:decimal_digits m) :radix (:num_prec_radix m) :default (:column_def m)
                     :autoincrement? (= "YES" (:is_autoincrement m))}})

(defn table-types
  [^DatabaseMetaData md]
  (mapv :table_type (resultset-seq (.getTableTypes md ))))

(defn primary-keys
  [^DatabaseMetaData md s]
  (when md
    (when-let [s (resultset-seq (.getPrimaryKeys md nil nil s))]
      (mapv :column_name s))))

(defn table-constraints
  [^DatabaseMetaData md s]
  {:primary-keys (primary-keys md s)})

(defn columns
  [^DatabaseMetaData md s]
  (into {} (map column (resultset-seq (.getColumns md nil nil s nil)))))

(defn schema
  [^DatabaseMetaData md s]
  (let [v (columns md s)]
    (when (seq v)
      (merge {:constraints (table-constraints md s)}
             {:columns v}))))

(defn tables
  [^DatabaseMetaData md]
  (resultset-seq (.getTables md nil nil nil nil)))

(defn public-table-names
  [^DatabaseMetaData md]
  (mapv :table_name (filter #(not= "SYSTEM TABLE" (:table_type %)) (tables md))))

(defn schemas
  [^Connection conn]
  (let [md (.getMetaData conn)
        v (public-table-names md)]
    (zipmap v (map #(schema md %) v))))

(defn select-star-statement
  [t]
  (str "SELECT * FROM " t))

(defn select-star
  [^Connection conn t]
  (let [st (.createStatement conn)
        rs (.executeQuery st (select-star-statement t))]
    (vec (raw-resultset-seq rs))))

(defn data
  ([^Connection conn] (data conn (public-table-names (.getMetaData conn))))
  ([^Connection conn v]
   (zipmap v (map #(select-star conn %) v))))
