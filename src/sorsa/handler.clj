(ns sorsa.handler
  (:require [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :as jetty]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as sql]
            [environ.core :refer [env]]))

(def db (env :database-url))

(defroutes app-routes
  (GET "/" []
    (io/file "resources/public/index.html"))
  
  (GET "/:folder/:document" [folder document]
    (->> (jdbc/query db
                     (sql/format {:select [:content]
                                    :from [:document]
                                    :where [:and
                                            [:= :name document]
                                            [:= :folder folder]]})
                     {:raw? true})
         (map :content)))
  
  (GET "/:folder" [folder]
    (->> (jdbc/query db
                     ["SELECT * FROM document WHERE folder = ?" folder]
                     {:raw? true})
         (map #(hash-map :name (:name %)
                         :size (count (:content %))))
         (json/write-str)
         (hash-map :status 200
                   :headers {"Content-Type" "application/json; charset=utf-8"}
                   :body)))
  
  (POST "/:folder/:document" [folder document content]
    (jdbc/insert! db :document {:name document
                                :folder folder
                                :content content}))
  
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app-routes) {:port port :join? false})))