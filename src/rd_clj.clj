(ns rd-clj
  (:gen-class 
   :extends javax.servlet.http.HttpServlet)
  (:use [compojure.core :only [defroutes GET]]
        [ring.util.servlet :only [defservice]])
  (:require [rd-clj.view.hello :as hello]
            [rd-clj.view.entry :as entry]
            [rd-clj.view.tag :as tag]))

(defroutes app
  (GET "/entry/create" req (entry/create req))
  (GET "/entry/view/:id" req (entry/view req))
  (GET "/entry/edit/:id" req (entry/edit req))
  (GET "/entry/delete/:id" req (entry/delete req))
  (GET "/entry/list" _ (entry/list))
  (GET "/tag/:tag" req (tag/list-dicts req))
  (GET "/*" _ (hello/hello)))

(defservice app)