(defproject rd-clj "0.1.0-SNAPSHOT"
  :description "the Reverse Dictionay of Clojure"
  :repositories {"gaejtools" "http://gaejtools.sourceforge.jp/maven/repository",
                 "scala-tools" "http://scala-tools.org/repo-releases"}
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]]
  :dependencies [[org.clojure/clojure "1.1.0"]
                 [org.clojure/clojure-contrib "1.1.0"]
                 [compojure "0.4.0-SNAPSHOT"]
                 [am.ik/clj-gae-ds "0.2.0-SNAPSHOT"]
                 [am.ik/clj-gae-users "0.1.0-SNAPSHOT"]
                 [com.google.appengine/appengine-api-1.0-sdk "1.3.2"]
                 [ring/ring-core "0.2.0"]
                 [ring/ring-servlet "0.2.0"]
                 [ring/ring-jetty-adapter "0.2.0"]
                 [hiccup/hiccup "0.2.3"]
                 [org.markdownj/markdownj "0.3.0-1.0.2b4"]]
  :dev-dependencies [[leiningen/lein-swank "1.1.0"]
                     [am.ik/clj-gae-testing "0.1.0"]]
  :compile-path "war/WEB-INF/classes/" 
  :library-path "war/WEB-INF/lib/"
  :namespaces [rd-clj
               rd-clj.utils
               rd-clj.logger
               rd-clj.daccess.dict
               rd-clj.validator.validator
               rd-clj.view.entry
               rd-clj.view.layout
               rd-clj.view.hello
               rd-clj.view.error])