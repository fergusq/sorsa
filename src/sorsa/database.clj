(ns sorsa.database
  (:require [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]))

(def db (env :database-url))

; Database queries

(defn check-password? [folder password]
  (->> (jdbc/query db
                   ["SELECT password = ? AS matches FROM folder WHERE name = ?"
                    password
                    folder])
       (every? :matches)))

(defn query-content [folder document]
  (jdbc/query db
              ["SELECT content FROM document WHERE name = ? AND folder = ?" document folder]
              {:row-fn :content
               :result-set-fn first}))

(defn query-folder-listing [folder]
  (jdbc/query db
              ["SELECT * FROM document WHERE folder = ?" folder]
              {:row-fn #(hash-map :name (:name %)
                                  :size (count (:content %)))}))

(defn query-exists-folder? [folder]
  (>= 1 (jdbc/query db
                    ["SELECT COUNT(*) AS count FROM folder WHERE name = ?" folder]
                    {:row-fn :count
                     :result-set-fn first})))

(defn delete-document! [folder document]
  (jdbc/execute! db ["DELETE FROM document WHERE folder = ? AND name = ?" folder document]))

(defn insert-document! [document-obj]
  (jdbc/insert! db :document document-obj))

(defn insert-folder! [folder-obj]
  (jdbc/insert! db :folder folder-obj))

; Table creation

(def ^:private document-table-ddl
  (jdbc/create-table-ddl :document
                         [[:name "varchar(100) primary key not null"]
                          [:folder "varchar(100) not null"]
                          [:content "text not null"]]))

(def ^:private folder-table-ddl
  (jdbc/create-table-ddl :folder
                         [[:name "varchar(100) primary key not null"]
                          [:password "text"]]))

(defn create-tables [db]
  (jdbc/db-do-commands db [document-table-ddl
                           folder-table-ddl
                           "CREATE INDEX name_idx ON document ( name, folder );"]))

(defn -main []
  (create-tables db))