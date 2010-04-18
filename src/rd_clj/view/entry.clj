(ns rd-clj.view.entry
  (:use [hiccup.core :only [html h]]
        [ring.util.response :only [redirect]]
        [clojure.contrib.str-utils :only [str-join]]
        [am.ik.clj-gae-ds.core]
        [am.ik.clj-gae-users.core]
        [rd-clj.utils]
        [rd-clj.view.layout]
        [rd-clj.daccess.dict])
  (:import [com.petebevin.markdown MarkdownProcessor]
           [com.google.appengine.api.datastore Text]
           [com.google.appengine.api.users User]
           [java.util TimeZone])
  (:refer-clojure :exclude [list]))

(declare create view edit delete list list-by-tag)
(TimeZone/setDefault (TimeZone/getTimeZone "JST"))

(defn show-tags [tags]
  (if-not (empty? tags) 
    (let [last-tag (last tags)]
      [:span      
       "["
       (for [tag tags] 
         [:span [:a {:href (format (fn->path-fmt list-by-tag) (url-enc tag))} tag] 
          (if-not (= tag last-tag) "/")])
       "]"])))

(defn show-dicts [dicts]
  (for [dict dicts]
    (let [ttl (get-prop dict :title)
          tags (get-prop dict :tags)
          id (get-id (get-key dict))]
      [:ul
       [:li [:a {:href (format (fn->path-fmt view) id)} ttl] " " (show-tags tags)]])))

(defn #^{:auth true, 
         :validate {:submit "do" 
                    :target {"title" [[:require]], 
                             "content" [[:require]]}}}
  create [req]
  (let [do? (get-in req [:params "do"])
        title (get-in req [:params "title"])
        #^String tags (get-in req [:params "tags"])
        content (get-in req [:params "content"])]
    (if do?
      (let [tag-list (map #(.trim %) (.split tags "/"))
            now (java.util.Date.)]
        (create-dict (map-entity "dict" :title title 
                                        :content (Text. content)
                                        :creator (get-current-user)
                                        :tags tag-list 
                                        :created now
                                        :updated now))
        (redirect "/"))
      (wmd-layout {}
        [:form {:method :post}
         [:dl
          [:dt "title:"]
          [:dd [:input {:type :text :name :title :value title}]]
          [:dt "content: (" [:a {:href "http://daringfireball.net/projects/markdown/syntax" :target "_blank"} "markdown"]
           " syntax is supported)"]
          [:dd [:textarea {:name :content} content]]
          [:dt "tags:"]
          [:dd [:input {:type :text :name :tags :value tags}]]
          [:dt "author:"]
          [:dd (.getNickname (get-current-user))]
          [:dt "submit:"]
          [:dd [:input {:type :submit :name :do}]]]]
        [:hr]
        [:h3 "Preview"]
        [:div {:class "wmd-preview"}]
        ))))

(defn 
  #^{:params [:id]}
  view [req]
  (let [id (get-in req [:params :id])
        dict (get-dict id)
        #^MarkdownProcessor mp (MarkdownProcessor.)]
    (highlight-layout {:title (get-h dict :title)}
      [:h2 (get-h dict :title)]
      [:p (.markdown mp (adhoc-esc (.getValue #^Text (get-prop dict :content))))]
      [:hr]
      [:p "tags: " (show-tags (get-prop dict :tags))]
      [:p "created by: " (h (.getNickname #^User (get-prop dict :creator)))
       (let [updater (get-prop dict :updater)]
         (if updater (str " updated by: " (h (.getNickname #^User updater)))))]
      (when (get-current-user)
        [:p " [" [:a {:href (format (fn->path-fmt edit) id)} "EDIT"] "]"
         " [" [:a {:href (format (fn->path-fmt delete) id)} "DEL"] "]"])
      [:p "created at: " (date (get-prop dict :created)) 
       " updated at: " (date (get-prop dict :updated))])))

  
(defn #^{:auth true, :params [:id],
         :validate {:submit "do" 
                    :target {"title" [[:require]], 
                             "content" [[:require]]}}} 
  edit [req]
  (let [do? (get-in req [:params "do"])
        id (get-in req [:params :id])
        dict (ds-get (create-key "dict" (Long/valueOf id)))
        title (get-in req [:params "title"])
        #^String tags (get-in req [:params "tags"])
        content (get-in req [:params "content"])
        updated?  (get-in req [:params "updated"])]
    (if do?
      (do         
        (doto dict
          (set-prop :title title)
          (set-prop :content (Text. content))
          (set-prop :updater (get-current-user)))
        (update-dict dict (if tags (map #(.trim #^String %) (.split tags "/"))) updated?)
        (redirect (format (fn->path-fmt edit) id)))
      (wmd-layout {}
        [:h2 [:a {:href (format (fn->path-fmt view) id)} (or title (get-prop dict :title) "")]]
        [:form {:method :post}
         [:dl
          [:dt "title:"]
          [:dd [:input {:type :text :name :title :value (or title (get-prop dict :title) "")}]]
          [:dt "content: (" [:a {:href "http://daringfireball.net/projects/markdown/syntax" :target "_blank"} "markdown"]
           " syntax is supported)"]
          [:dd [:textarea {:name :content} (or content (.getValue #^Text (get-prop dict :content)) "")]]
          [:dt "tags:"]
          [:dd [:input {:type :text :name :tags :value (or tags (str-join "/" (get-prop dict :tags)) "")}]]
          [:dt "updater:"]
          [:dd (.getNickname (get-current-user))]
          [:dt "update date?:"]
          [:dd "update date " [:input {:type :checkbox :name :updated}]]
          [:dt "submit:"]
          [:dd [:input {:type :submit :name :do}]]]]
        [:hr]
        [:h3 "Preview"]
        [:div {:class "wmd-preview"}]
        ))))
  
(defn list []
  (default-layout {}
    (show-dicts (get-dicts))))

(defn #^{:auth true, :params [:id]} 
  delete [req]
  (let [id (get-in req [:params :id])]
    (delete-dict id)
    (redirect "/")))

(defn 
  #^{:route "tag", :params [:tag]}
  list-by-tag [req]
  (let [tag (url-dec (get-in req [:params :tag]))]
    (default-layout {} 
      [:h2 "Tagged by " (h tag) " ..."]
      (show-dicts (tagname->dicts tag)))))