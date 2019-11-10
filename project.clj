(defproject sorsa "0.1.0-SNAPSHOT"
  :description "Source Storage Application"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "9.4.1211"]
                 [com.bhauman/rebel-readline "0.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [environ "1.1.0"]]
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler sorsa.handler/app}
  :uberjar-name "sorsa.jar"
  :profiles {:production {:env {:production true}}})
