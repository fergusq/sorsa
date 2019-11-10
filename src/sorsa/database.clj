(ns sorsa.database
  (:require [clojure.java.jdbc :as jdbc]))

(def ^:private document-table-ddl
  (jdbc/create-table-ddl :document
                         [[:name "varchar(100) primary key not null"]
                          [:folder "varchar(100) not null"]
                          [:content "text not null"]]))

(defn create-tables [db]
  (jdbc/db-do-commands db [document-table-ddl
                           "CREATE INDEX name_idx ON document ( name, folder );"]))

(defn -main [db]
  (create-tables db))