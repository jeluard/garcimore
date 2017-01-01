Des fois ca marche.

# Garcimore [![License](http://img.shields.io/badge/license-EPL-blue.svg?style=flat)](https://www.eclipse.org/legal/epl-v10.html)

[Usage](#usage)

A Clojure(Script) library to compare and synchronize SQL databases (DDL and DML).

## Usage

Generate diffs from database snapshots:

```clojure
(ns my-app
  (:require [garcimore.dml :as gdml]))

(gdml/diff-tables {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
                  {"FRUIT"
                   [{"ID" 1 "APPEARANCE" "rosy" "COST" 24}
                    {"ID" 2 "APPEARANCE" "round" "COST" 49}
                    {"ID" 4 "APPEARANCE" "rosy" "COST" 49}]}
                  {"FRUIT"
                   [{"ID" 3 "APPEARANCE" "rosy" "COST" 24}
                    {"ID" 1 "APPEARANCE" "rosy" "COST" 24}
                    {"ID" 4 "APPEARANCE" "round" "COST" 24}]})

; => {"FRUIT" {:inserted [{"ID" 3 "APPEARANCE" "rosy" "COST" 24}]
;              :updated [{"ID" 4 "APPEARANCE" ["rosy" "round"] "COST" [49 24]}]
;              :deleted [{"ID" 2 "APPEARANCE" "round" "COST" 49}]}}
```

Data can be extracted from an existing database using JDBC drivers:

```clojure
(ns my-app
  (:require [garcimore.jdbc :as gjdbc]))


; {"FRUIT" {:columns {"APPEARANCE" {:type :varchar ...}
;                     "ID" {:type :integer ...}
;                     "COST" {:type :integer ...}}
;           :constraints {:primary-keys ["ID"]}}}

(let [conn ..] ; a JDBC Connection
  (gjdbc/data conn))

; => {"FRUIT" [{"ID" 1 "APPEARANCE" "rosy" "COST" 24}]}
```

Generate SQL statements from diffs (using liquibase):

```clojure
(ns my-app
  (:require [garcimore.liquibase :as gliq]))

(gliq/diff->sql
  {"FRUIT" {:constraints {:primary-keys ["ID"]}}}
  {"FRUIT" {:inserted [{"APPEARANCE" "round"
                        "COST"       49
                        "ID"         1}
                       {"APPEARANCE" "rosy"
                        "COST"       24
                        "ID"         2}]}}
  :mariadb)

; => '("INSERT INTO FRUIT (APPEARANCE, COST, ID) VALUES ('round', 49, 1)"
;      "INSERT INTO FRUIT (APPEARANCE, COST, ID) VALUES ('rosy', 24, 2)")'
```

## License

Copyright (C) 2016 - 2017 Julien Eluard

Distributed under the Eclipse Public License, the same as Clojure.
