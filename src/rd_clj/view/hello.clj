(ns rd-clj.view.hello
  (:use [hiccup.core :only [html h]]
        [rd-clj.view.layout]
        [rd-clj.daccess.dict]))

(defn hello []
  (default-layout {}
    [:p 
     [:h2 "逆引き一覧"]
     (show-dicts (get-dicts 10))
     [:p [:a {:href "/entry/list"} "全て見る"]]
     [:hr]
     [:a {:href "/entry/create"} "新規作成"]]))
