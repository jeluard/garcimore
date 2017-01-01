(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha11"]
                   [org.mariadb.jdbc/mariadb-java-client "1.4.6"]
                   [org.liquibase/liquibase-core "3.5.1"]
                   [org.clojure/java.jdbc "0.6.2-alpha3"]
                   [com.h2database/h2 "1.4.192"]
                   [boot-codox "0.10.1" :scope "test"]])

(require '[codox.boot :refer [codox]])

; https://github.com/viebel/codox-klipse-theme
; https://github.com/xsc/codox-theme-rdash

(deftask docs []
  (comp
    (codox
      :name "Example Project"
      :description "FIXME: write description"
      ;:source-uri "https://github.com/weavejester/codox/blob/{version}/codox.example/{filepath}#L{basename}-{line}"
      :metadata {:doc/format :markdown}
      #_:themes #_[:default [:klipse
               {:klipse/external-libs
               "https://raw.githubusercontent.com/my_user/my_repo/master/src/"
                :klipse/require-statement
                "(ns my.test
                (:require [my_repo.my_ns :as my_ns :refer [my_func]]))"}]])
    (target)))