(ns rd-clj
  (:gen-class 
   :extends javax.servlet.http.HttpServlet)
  (:use [compojure.core :only [defroutes GET POST ANY]]
        [ring.util.servlet :only [defservice]]
        [rd-clj.common])
  (:require [rd-clj.view.layout :as layout]
            [rd-clj.view.hello :as hello]
            [rd-clj.view.entry :as entry]))

(defroutes+ app
  (ANY entry/create)
  (GET entry/view)
  (ANY entry/edit)
  (GET entry/delete)
  (GET entry/list)
  (GET entry/list-by-tag)
  (ANY entry/version)
  (GET login)
  (GET logout)
  (GET hello/hello))

(defservice app)