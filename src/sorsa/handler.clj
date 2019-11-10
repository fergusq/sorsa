(ns sorsa.handler
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.adapter.jetty :as jetty]
            [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]
            [sorsa.database :refer [db
                                    check-password?
                                    query-content
                                    query-folder-listing
                                    query-exists-folder?]]))

; DB access functions

(def response-401 {:status 401
                   :headers {"Content-Type" "text/html; charseT=utf-8"}
                   :body "<!DOCTYPE html><title>401 Forbidden</title><h1>401 Forbidden</h1>"})

(defn response-json [data] {:status 200
                            :headers {"Content-Type" "application/json; charset=utf-8"}
                            :body (json/write-str data)})

(defn escape-html [text]
  (str/escape text {\< "&lt;"
                    \> "&gt;"
                    \& "&amp;"}))

(defroutes app-routes
  (GET "/" []
    (io/file "resources/public/index.html"))
  
  (GET "/:folder/document/:document" [folder document password]
    (if (check-password? folder password)
      (query-content folder document)
      response-401))
  
  (GET "/:folder/list" [folder password]
    (if (check-password? folder password)
      (response-json (query-folder-listing folder))
      response-401))
  
  (GET "/:folder/view" [folder password]
    (if (check-password? folder password)
      (let [documents (query-folder-listing folder)]
        (str "<!DOCTYPE html>"
             "<title>" (escape-html folder) "</title>"
             "<h1>" (escape-html folder) "/</h1>"
             "<table><tr><th>Document</th><th>Size</th>"
             (str/join (for [document documents]
                         (str "<tr><td>" (escape-html (:name document)) "</td><td>" (:size document) "</td>")))
             "</table>"))
      response-401))
  
  (POST "/:folder/document/:document" [folder document content password]
    (case (query-exists-folder? folder)
      1 (if (check-password? folder password)
          (do (jdbc/execute! db ["DELETE FROM document WHERE folder = ? AND name = ?" folder document])
              (jdbc/insert! db :document {:name document
                                          :folder folder
                                          :content content}))
          response-401)
      (do (jdbc/insert! db :folder {:name folder
                                    :password password})
          (jdbc/insert! db :document {:name document
                                      :folder folder
                                      :content content}))))
  
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app-routes) {:port port :join? false})))