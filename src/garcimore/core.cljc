(ns garcimore.core
  (:require [clojure.set :as set]
            [clojure.spec :as spec]))

(defn mzipmap
  "Returns a map with the keys mapped to the corresponding vals."
  {:added "1.0"
   :static true}
  [keys vals]
  (loop [map {}
         ks (seq keys)
         vs (seq vals)]
    (if (and ks vs)
      (let [v (first vs)]
        (recur (if v (assoc map (first ks) v) map)
               (next ks)
               (next vs)))
      map)))

(spec/def ::seq-of-map (spec/+ map?))
(spec/def ::tables (spec/+ string?))
(spec/def ::data map?)
(spec/def ::data #(and (map? %) (every? string? (keys %)) (every? (fn [a] (spec/valid? ::seq-of-map a)) (vals %))))

(defn null-allowed?
  [m o]
  (and (nil? o) (:nullable? m)))

(defmulti valid-type? (fn [m _] (:type m)))
(defmethod valid-type? :garcimore/varchar [m ^String v] (or (null-allowed? m v)
                                                            (and (string? v) (< (.length v) (:column-size m)))))
(defmethod valid-type? :default [_ v] (println "unknown?" v) false)

(defn valid-column?
  [m k v]
  (valid-type? (get m k) v))

(defn valid-columns?
  [mc m]
  (every? (fn [[k v]] (valid-column? mc k v)) m))

(defn valid?
  "Returns true if data is valid for schema"
  [ms m]
  (and
    (set/subset? (set (keys m)) (set (keys (:columns ms))))
    (valid-columns? (:columns ms) m))

  ; TODO
  ; duplicate value based on primary keys -> false
  )
