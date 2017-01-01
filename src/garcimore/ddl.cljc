(ns garcimore.ddl
  (:require [clojure.spec :as spec]
            [clojure.set :as set]))

(spec/def ::seq-of-map (spec/+ map?))

(spec/fdef primary-key-values
           :args (spec/cat :pks (spec/+ string?) :data :garcimore.core/data)
           :ret nil)

(defn primary-key-values
  [s m]
  #_
  {:pre  [(spec/assert ::data m)]
   :post [(spec/assert ::data %) (every? (comp not nil?) (vals %))]}
  ; TODO s can't be empty
  (select-keys m s))

(defn keys-values-map
  [s v]
  (into {} (map (fn [m] (let [pkv (primary-key-values s m)] [pkv m])) v)))

(defn diff-table-schema
  "
  ~~~klipse\n(my_func 1 2 3)\n~~~

  ```(max 1 2)```

  # Titre

  * aa
  * aaa
  "
  [om nm]
  (let [okv (set (keys (:columns om)))
        nkv (set (keys (:columns nm)))
        ukv (set/intersection okv nkv)
        dkv (set/difference okv nkv)
        ikv (set/difference nkv okv)
        ;uv (diff-table-updated-rows ukv okm nkm)
        dv (select-keys (:columns om) dkv)
        iv (select-keys (:columns nm) ikv)]
    (merge #_(if (seq uv)
             {:updated uv})
           (if (seq dv)
             {:deleted {:columns dv}})
           (if (seq iv)
             {:inserted {:columns iv}}))))

(defn diff-schema
  [m1 m2]
  )
