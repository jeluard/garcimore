(ns garcimore.dml
  (:require [clojure.set :as set]
            [clojure.spec :as spec]
            [garcimore.core :as gco]
            [garcimore.ddl :as gddl]
            [clojure.spec :as spec]
            [clojure.spec :as spec]
            [clojure.spec :as spec]))

; Identity is defined by primary key.
; When no primary key is defined for a table rows are not identified and updated in such way that changes are minimized.
; If previous states contains more lines, extra are deleted. If it contains less lines, new data is inserted.

(defn- updates-for
  [k om nm]
  (let [ov (get om k)
        nv (get nm k)]
    (if (not= ov nv)
      [ov nv])))

(defn diff-row
  [om nm]
  (let [ok (set (keys om))
        nk (set (keys nm))
        ik (set/intersection ok nk)]
    (gco/mzipmap ik (map #(updates-for % om nm) ik))))

(spec/def :garcimore/row-value (spec/or :string string? :boolean boolean? :integer integer? :float float?))
(spec/def :garcimore/row (spec/map-of string? :garcimore/row-value))

(spec/def :garcimore/row-diff (spec/map-of string? (spec/tuple :garcimore/row-value :garcimore/row-value)))

(spec/fdef diff-row
           :args (spec/cat :row :garcimore/row :row :garcimore/row)
           :ret nil)

(defn diff-table-updated-rows
  [ikv okm nkm]
  (filterv seq (mapv #(let [m (diff-row (get okm %) (get nkm %))]
                       (if (seq m) (merge % m))) ikv)))

(defn diff-table-with-primary-keys
  [s ov nv]
  (let [okm (gddl/keys-values-map s ov)
        nkm (gddl/keys-values-map s nv)
        okv (set (keys okm))
        nkv (set (keys nkm))
        ukv (set/intersection okv nkv)
        dkv (set/difference okv nkv)
        ikv (set/difference nkv okv)
        uv (diff-table-updated-rows ukv okm nkm)
        dv (mapv #(get okm %) dkv)
        iv (mapv #(get nkm %) ikv)]
    (merge (if (seq uv)
             {:updated uv})
           (if (seq dv)
             {:deleted dv})
           (if (seq iv)
             {:inserted iv}))))

(spec/fdef diff-table-with-primary-keys
           :args (spec/cat :ids (spec/coll-of string?) :row (spec/or :nil nil? :rows (spec/coll-of :garcimore/row)) :row (spec/coll-of :garcimore/row))
           :ret nil)

(defn diff-table
  [s ms omm nmm]
  (let [om (get omm s)
        nm (get nmm s)
        pks (get-in ms [s :constraints :primary-keys])]
    (diff-table-with-primary-keys pks om nm)))

(defn diff-tables
  ([ms omm nmm] (diff-tables (set/union (set (keys omm)) (set (keys nmm))) ms omm nmm))
  ([tv ms omm nmm]
   (reduce (fn [m s] (update m s (fn [_] (diff-table s ms omm nmm))))
           {} tv)))
