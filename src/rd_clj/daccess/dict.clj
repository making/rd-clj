(ns rd-clj.daccess.dict
  (:import [com.google.appengine.api.datastore Entity])
  (:use [hiccup.core :only [html h]]
        [am.ik.clj-gae-ds.core]
        [rd-clj.utils]
        [rd-clj.view.layout]))

(def #^{:private true}
     +max-query-num+ 500)

(defn tagname->dicts 
  ([tagname num]
     (vals (ds-get (map (fn [#^Entity tg] (.getParent tg)) (take num (query-seq (-> (q "tag") (flt "name" = tagname))))))))
  ([tagname]
     (tagname->dicts tagname +max-query-num+)))

(defn dict->tags 
  ([#^Entity dict num]
     (vals (ds-get (map (fn [#^Entity d] (.getKey d)) (take num (query-seq (q "tag" (.getKey dict))))))))
  ([#^Entity dict]
     (dict->tags dict +max-query-num+)))

(defn create-dict [#^Entity entity]
  (with-transaction 
    (let [k (ds-put entity)]
      (ds-put (map #(map-entity "tag" :name % :parent k) (get-prop entity :tags))))))

(defn update-dict [dict new-tags update-date?]
  (with-transaction
    (ds-delete (map (fn [#^Entity tg] (.getKey tg)) (dict->tags dict))) ; delete old tags
    (set-prop dict :tags new-tags) ; retag
    (if update-date? (set-prop dict :updated (java.util.Date.)))
    (let [k (ds-put dict)]
      (ds-put (map #(map-entity "tag" :name % :parent k) new-tags)))))

(defn get-dicts 
  ([num]
     (take num (query-seq (-> (q "dict") (srt "updated" :desc)))))
  ([]
     (get-dicts +max-query-num+)))

(defn get-dict [id]
  (ds-get (create-key "dict" (if (string? id) (Long/valueOf id) id))))

(defn delete-dict [id]
  (let [k (create-key "dict" (if (string? id) (Long/valueOf id) id))]
    (with-transaction
      (ds-delete k)
      (ds-delete (map (fn [#^Entity tg] (.getKey tg)) 
                      (query-seq (.setKeysOnly (query "tag" k))))))))

(defn show-dicts [dicts]
  (for [dict dicts]
    (let [ttl (get-prop dict :title)
          tags (get-prop dict :tags)
          id (.getId (.getKey dict))]
      [:ul
       [:li [:a {:href (str "/entry/view/" id)} ttl]
        " [" (for [tag tags] 
                  [:span [:a {:href (str "/tag/" (url-enc tag))} tag] "/"]) "]"]])))
