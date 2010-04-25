(ns rd-clj.view.entry
  (:use [hiccup.core :only [html h]]
        [hiccup.form-helpers]
        [ring.util.response :only [redirect]]
        [clojure.contrib.str-utils :only [str-join]]
        [am.ik.clj-gae-ds.core]
        [am.ik.clj-gae-users.core]
        [rd-clj.utils]
        [rd-clj.common]
        [rd-clj.view.layout]
        [rd-clj.daccess.dict])
  (:require [rd-clj.logger :as log])
  (:import [com.google.appengine.api.datastore Text]
           [com.google.appengine.api.users User]
           [java.util TimeZone])
  (:refer-clojure :exclude [list]))

(declare create view edit delete list list-by-tag version)

(defn show-tags [tags]
  (let [h-tags (map h tags)]
    (if-not (empty? h-tags) 
      (let [last-tag (last h-tags)]
        [:span      
         "["
         (for [tag h-tags] 
           [:span [:a {:href (format (fn->path-fmt list-by-tag) (url-enc tag))} tag] 
            (if-not (= tag last-tag) "/")])
         "]"]))))

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
        (log/info (fn->route create) [(get-nickname) (date now)])
        (create-dict (map-entity "dict" :title title 
                                        :content (Text. content)
                                        :creator (get-current-user)
                                        :tags tag-list 
                                        :created now
                                        :updated now))
        (redirect "/"))
      (wmd-layout {}
        (form-to [:post]
          [:dl
           [:dt "Title:"]
           [:dd [:input {:type :text :name :title :value title :class "input"}]]
           [:dt "Content: (" [:a {:href "http://daringfireball.net/projects/markdown/syntax" :target "_blank"} "markdown"]
            " syntax is supported)"]
           [:dd [:textarea {:name :content} content]]
           [:dt "Tags: (separated by \"/\")"]
           [:dd [:input {:type :text :name :tags :value tags :class "input"}]]
           [:dt "Author:"]
           [:dd (get-nickname)]
           [:dt "Submit:"]
           [:dd [:input {:type :submit :name :do}]]])
        [:hr]
        [:h3 "Live Preview"]
        [:div {:class "wmd-preview"}]
        ))))

(defn 
  #^{:params [:id]}
  view [req]
  (TimeZone/setDefault (TimeZone/getTimeZone "JST"))
  (let [id (get-in req [:params "id"])
        dict (get-dict id)]
    (highlight-layout {:title (get-h dict :title)}
      [:h2 (get-h dict :title)]
      [:p (get-content dict)]
      [:hr]
      [:p "Tags: " (show-tags (get-prop dict :tags))]
      [:p "Created By: " (h (get-nickname (get-prop dict :creator)))
       (let [updater (get-prop dict :updater)]
         (if updater (str " Updated By: " (h (get-nickname #^User updater)))))]
      (when (get-current-user)
        [:p " [" [:a {:href (format (fn->path-fmt edit) id)} "EDIT"] "]"
         " [" [:a {:href (format (fn->path-fmt delete) id) :onclick "return confirm('Are you sure you wish to delete this entry?')"} "DEL"] "]"])
      [:p "Created At: " (date (get-prop dict :created)) 
       " Updated At: " (date (get-prop dict :updated))])))

(defn #^{:auth true, :params [:id],
         :validate {:submit "do" 
                    :target {"id" [[:require :long]],
                             "title" [[:require]], 
                             "content" [[:require]]}}} 
  edit [req]
  (let [id (get-in req [:params "id"])
        dict (ds-get (create-key "dict" (Long/valueOf id)))
        title (get-in req [:params "title"])
        #^String tags (get-in req [:params "tags"])
        content (get-in req [:params "content"])
        updated? (get-in req [:params "updated"])
        version? (get-in req [:params "version"])
        versions (dict->versions dict 20)
        do? (get-in req [:params "do"])]
    (if do?
      (do
        (log/info (fn->route edit) [(get-nickname) (date (java.util.Date.)) id])
        ;; do update
        (update-dict dict 
                     {:title title, 
                      :content (Text. content), 
                      :updater (get-current-user),
                      :updated (if updated? (java.util.Date.))}
                     (if tags (map #(.trim #^String %) (.split tags "/")))
                     (or version? (not= (get-current-user) (or (get-prop dict :updater) (get-prop dict :creator)))))
        (redirect (format (fn->path-fmt edit) id)))
      (wmd-layout {}
        [:h2 [:a {:href (format (fn->path-fmt view) id)} (or title (get-prop dict :title) "")]]        
        (form-to [:post (format (fn->path-fmt edit) id)]
         [:dl
          [:dt "Title:"]
          [:dd [:input {:type :text :name :title :value (or title (get-prop dict :title) "") :class "input"}]]
          [:dt "Content: (" [:a {:href "http://daringfireball.net/projects/markdown/syntax" :target "_blank"} "markdown"]
           " syntax is supported)"]
          [:dd [:textarea {:name :content} (or content (.getValue #^Text (get-prop dict :content)) "")]]
          [:dt "Tags: (separated by \"/\")"]
          [:dd [:input {:type :text :name :tags :value (or tags (str-join "/" (get-prop dict :tags)) "") :class "input"}]]
          [:dt "Updater:"]
          [:dd (get-nickname)]
          [:dt "Update Date:"]
          [:dd [:span "save updated date? "] [:input {:type :checkbox :name :updated}]]
          [:dt "Version: (If updater is changed, then previous version is automatically saved.)"]
          [:dd [:span "save previous version? "] [:input {:type :checkbox :name :version}]]
          [:dt "Submit:"]
          [:dd [:input {:type :submit :name :do}]]])
        [:hr]
        (when-not (empty? versions)
          [:div 
           [:h3 "Saved Versions"]
           (form-to [:post (format (fn->path-fmt version) id)]
             (drop-down :version-id (map (fn [x] [(get-prop x :select-view) (get-id (get-key x))]) versions))
             " "
             [:input {:type :submit :name :do :value "apply this version"}])
           [:hr]])
        [:h3 "Live Preview"]
        [:div {:class "wmd-preview"}]
        ))))

(defn #^{:auth true, :params [:id],
         :validate {:submit "do" 
                    :target {"id" [[:require :long]],
                             "version-id" [[:require :long]]}}}
  version [req]
  (let [id (get-in req [:params "id"])
        dict (ds-get (create-key "dict" (Long/valueOf id)))
        version-id (get-in req [:params "version-id"])
        version (get-version dict version-id)]
      ;; apply version
      (edit {:params {"id" id,
                      "title" (get-prop version :title),
                      "content" (.getValue #^Text (get-prop version :content)),
                      "tags" (str-join "/" (get-prop version :tags))
                      }})))

(defn list []
  (default-layout {}
    (show-dicts (get-dicts))))

(defn #^{:auth true, :params [:id]} 
  delete [req]
  (let [id (get-in req [:params "id"])]
    (log/info (fn->route delete) [(get-nickname) (date (java.util.Date.)) id])
    (delete-dict id)
    (redirect "/")))

(defn 
  #^{:route "tag", :params [:tag]}
  list-by-tag [req]
  (let [tag (url-dec (get-in req [:params "tag"]))]
    (default-layout {} 
      [:h2 "Tagged by " (h tag) " ..."]
      (show-dicts (tagname->dicts tag)))))