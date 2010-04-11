(ns rd-clj.view.layout
  (:use [hiccup.core :only [html h]]
        [am.ik.clj-gae-ds.core]
        [rd-clj.view.error]))

(defmacro layout-template [{title :title} & body]
  (let [default-ttl "逆引きClojure"
        ttl (or title default-ttl)
        css "http://clojure-users.org/css/style.css"]
    `(html [:html 
            [:head 
             [:title ~ttl]
             [:link {:rel "stylesheet" :type "text/css" :href ~css :media "screen"}]]            
            [:div {:id "wrap"}
             [:div {:id "header"}
              [:h1 [:a {:href "/"} ~default-ttl]]]
             [:div {:id "content"}
              [:div  {:class "left"}
               ~@body]
              [:div  {:class "right"}]
              [:div  {:style "clear:both;"}]
              ]]])))

(defmacro default-layout [{title :title} & body]
  `(try 
    (layout-template {:title ~title} ~@body)
    (catch Throwable e#
      (let [handled# (handle-error e#)]
        (layout-template {:title "Error!!"} handled#)))))
