(ns sorsa.handler
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [compojure.core :refer [defroutes GET PUT]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [environ.core :refer [env]]
            [hiccup.page :refer [html5]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [sorsa.database :refer [check-password?
                                    query-content
                                    query-folder-listing
                                    query-exists-folder?
                                    delete-document!
                                    insert-document!
                                    insert-folder!]]))

(def response-403 {:status 403
                   :headers {"Content-Type" "text/html; charseT=utf-8"}
                   :body (html5
                          [:title "403 Forbidden"]
                          [:h1 "403 Forbidden"]
                          [:p "Invalid or missing password."
                           " Please use the " [:code "password"] " query parameter."])})

(defn response-json [data] {:status 200
                            :headers {"Content-Type" "application/json; charset=utf-8"
                                      "Access-Control-Allow-Origin" "*"}
                            :body (json/write-str data)})

(defn response-content [data] {:status 200
                               :headers {"Content-Type" "text/html; charset=utf-8"
                                         "Access-Control-Allow-Origin" "*"}
                               :body data})

(defroutes app-routes
  (GET "/" []
    (io/file "resources/public/index.html"))
  
  (GET "/folder/:folder/document/:document" [folder document password]
    (if (check-password? folder password)
      (response-content (query-content folder document))
      response-403))
  
  (GET "/folder/:folder/list" [folder password]
    (if (check-password? folder password)
      (response-json (query-folder-listing folder))
      response-403))
  
  (GET "/folder/:folder/view" [folder password]
    (if (check-password? folder password)
      (let [documents (query-folder-listing folder)]
        (html5
         [:title folder]
         [:h1 "Folder listing - " folder]
         [:table
          [:tr [:th "Document"] [:th "Size"]]
          (for [document documents]
            [:tr
             [:td [:a {:href (str
                              "/folder/" folder
                              "/document/" (:name document)
                              "?password=" password)}
                   (:name document)]]
             [:td (:size document)]])]))
      response-403))
  
  (PUT "/folder/:folder/document/:document" [folder document content password]
    (let [document-obj {:name document
                        :folder folder
                        :content content}]
      (if (query-exists-folder? folder)
        (if (check-password? folder password)
          (do (delete-document! folder document)
              (insert-document! document-obj)
              (response-json document-obj))
          response-403)
        (do (insert-folder! {:name folder
                             :password password})
            (insert-document! document-obj)
            (response-json document-obj)))))
  
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes api-defaults))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app-routes) {:port port :join? false})))
