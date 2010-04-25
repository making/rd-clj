(ns rd-clj.view.hello
  (:use [hiccup.core :only [html h]]
        [ring.util.response :only [redirect]]
        [rd-clj.view.layout]
        [am.ik.clj-gae-users.core]
        [rd-clj.daccess.dict]
        [rd-clj.view.entry :only [show-dicts]]))

(defn 
  #^{:route "*", :not-use-ns true}
  hello []
  (default-layout {}
    [:p 
     [:h2 "逆引き一覧"]
     (show-dicts (get-dicts 10 :updated :desc))
     [:p [:a {:href "/entry/list"} "全て見る"]]
     [:hr]
     [:a {:href "/entry/create"} "新規作成"]]))
