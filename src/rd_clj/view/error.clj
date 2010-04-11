(ns rd-clj.view.error
  (:use [hiccup.core :only [html h]]
        [am.ik.clj-gae-ds.core]
        [rd-clj.view.layout]))

(defmulti handle-error class)

(defmethod handle-error :default [#^Throwable err]
  (html 
   [:html
    [:h2  (.getMessage err)]
    [:p {:style "color:red;"}
     (str err)
     ]]))