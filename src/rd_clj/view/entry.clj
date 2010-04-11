(ns rd-clj.view.entry
  (:use [hiccup.core :only [html h]]
        [ring.util.response :only [redirect]]
        [clojure.contrib.str-utils :only [str-join]]
        [am.ik.clj-gae-ds.core]
        [rd-clj.utils]
        [rd-clj.view.layout]
        [rd-clj.daccess.dict])
  (:import [com.petebevin.markdown MarkdownProcessor])
  (:refer-clojure :exclude [list]))

(defn create [req]
  (let [do? (get-in req [:params "do"])
        title (get-in req [:params "title"])
        #^String tags (get-in req [:params "tags"])
        content (get-in req [:params "content"])
        author (get-in req [:params "author"])]
    (if do?
      (let [tag-list (map #(.trim %) (.split tags "/"))
            now (java.util.Date.)]
        (create-dict (map-entity "dict" :title title :content content :author author :tags tag-list :created now :updated now))
        (redirect "/"))
      (default-layout {}
        [:form
         [:dl
          [:dt "title:"]
          [:dd [:input {:type :text :name :title :value title}]]
          [:dt "content:"]
          [:dd [:textarea {:name :content :style "width:80%;height:300px;"} content]]
          [:dt "tags:"]
          [:dd [:input {:type :text :name :tags :value tags}]]
          [:dt "author:"]
          [:dd [:input {:type :text :name :author :value author}]]
          [:dt "submit:"]
          [:dd [:input {:type :submit :name :do}]]]]))))

(defn edit [req]
  (let [do? (get-in req [:params "do"])
        id (get-in req [:params :id])
        dict (ds-get (create-key "dict" (Long/valueOf id)))
        title (get-in req [:params "title"])
        #^String tags (get-in req [:params "tags"])
        content (get-in req [:params "content"])
        author (get-in req [:params "author"])
        updated? (get-in req [:params "updated"])]
    (if do?
      (do         
        (doto dict
          (set-prop :title title)
          (set-prop :content content)
          (set-prop :author author))
        (update-dict dict (if tags (map #(.trim %) (.split tags "/"))) updated?)
        (redirect (str "/entry/edit/" id)))
      (default-layout {}
        [:form
         [:dl
          [:dt "title:"]
          [:dd [:input {:type :text :name :title :value (or title (get-prop dict :title) "")}]]
          [:dt "content:"]
          [:dd [:textarea {:name :content :style "width:80%;height:300px;"} (or content (get-prop dict :content) "")]]
          [:dt "tags:"]
          [:dd [:input {:type :text :name :tags :value (or tags (str-join "/" (get-prop dict :tags)) "")}]]
          [:dt "author:"]
          [:dd [:input {:type :text :name :author :value (or author (get-prop dict :author) "")}]]
          [:dt "submit:"]
          [:dd [:input {:type :submit :name :do}]]]]))))

  
(defn view [req]
  (let [id (get-in req [:params :id])
        dict (get-dict id)
        #^MarkdownProcessor mp (MarkdownProcessor.)]
    (default-layout {:title (get-h dict :title)}
      [:h2 (get-h dict :title)]
      [:p (.markdown mp (get-h dict :content))]
      [:p (get-h dict :date) " " (get-h dict :author)
       " [" [:a {:href (str "/entry/edit/" id)} "E"] "]"
       " [" [:a {:href (str "/entry/delete/" id)} "D"] "]"]
      [:p "created: " (get-h dict :created) " updated: " (get-h dict :updated)])))

(defn list []
  (default-layout {}
    (show-dicts (get-dicts))))

(defn delete [req]
  (let [id (get-in req [:params :id])]
    (delete-dict id)
    (redirect "/")))