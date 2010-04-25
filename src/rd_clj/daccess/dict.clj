(ns rd-clj.daccess.dict
  (:use [am.ik.clj-gae-ds.core]
        [am.ik.clj-gae-users.core]
        [rd-clj.utils]
        [rd-clj.view.layout])
  (:require [rd-clj.logger :as log])
  (:import [java.util TimeZone]
           [com.google.appengine.api.datastore Text]
           [com.petebevin.markdown MarkdownProcessor]))

(def #^{:private true}
     +max-query-num+ 500)

(defn tagname->dicts 
  ([tagname num]
     (vals (ds-get (map get-parent (take num (query-seq (-> (q "tag") (flt :name = tagname))))))))
  ([tagname]
     (tagname->dicts tagname +max-query-num+)))

(defn cache-content [dict]
  (log/info "cache-content" [dict])
  (let [#^MarkdownProcessor mp (MarkdownProcessor.)
        md-content (.markdown mp (adhoc-esc (.getValue #^Text (get-prop dict :content))))]
    (mc-put "dict" (get-key dict) md-content)
    md-content))

(defn get-content [dict]
  (or (mc-get "dict" (get-key dict)) (cache-content dict)))

(defn dict->tags 
  ([dict num]
     (vals (ds-get (map get-key (take num (query-seq (q "tag" (get-key dict))))))))
  ([dict]
     (dict->tags dict +max-query-num+)))

(defn dict->versions 
  ([dict num]
;;     (vals (ds-get (map get-key (take num (query-seq (-> (q "version" (get-key dict)) (srt :updated :desc))))))))
     (vals (ds-get (map get-key (take num (query-seq (q "version" (get-key dict))))))))
  ([dict]
     (dict->versions dict +max-query-num+)))

(defn get-version [dict version-id]
  (ds-get (create-key (get-key dict) "version" (if (string? version-id) (Long/valueOf version-id) version-id))))

(defn create-version [dict]
  (log/info "create-version" [dict])
  (TimeZone/setDefault (TimeZone/getTimeZone "JST"))
  (let [d (java.util.Date.)
        updater (or (get-prop dict :updater) (get-prop dict :creator))]
    (map-entity "version" :title (get-prop dict :title)
                          :content (get-prop dict :content) 
                          :updater updater
                          :tags (get-prop dict :tags)
                          :updated d
                          :select-view   (str (date d) " - " (get-nickname updater))
                          :parent (get-key dict))))

(defn apply-version [dict version]
  (doseq [k [:title :content :updater :tags :updated]]    
    (set-prop dict k (get-prop version k)))
  dict)

(defn create-dict [entity]
  (log/info "create-dict" [entity])
  (with-transaction 
    (let [k (ds-put entity)]
      (ds-put (map #(map-entity "tag" :name % :parent k) (get-prop entity :tags))))
    (cache-content entity)))

(defn update-dict [dict new-params new-tags version?]
  (log/info "update-dict" [dict new-params new-tags version?])
  (with-transaction
    ;; save previous version
    (if version?
      (ds-put (create-version dict)))
    ;; set new params
    (doseq [[k v] new-params]
      (if v (set-prop dict k v))
      (if (= k :content) (cache-content dict)))
    ;; replace tags
    (ds-delete (map get-key (dict->tags dict))) ; delete old tags
    (set-prop dict :tags new-tags) ; retag
    (let [k (ds-put dict)]
      ;; update tags
      (ds-put (map #(map-entity "tag" :name % :parent k) new-tags)))))

(defn get-dicts 
  ([num sorted-by asc-or-desc]
     (take num (query-seq (-> (q "dict") (srt sorted-by asc-or-desc)))))
  ([num]
     (get-dicts num :title :asc))
  ([]
     (get-dicts +max-query-num+)))

(defn get-dict [id]
  (ds-get (create-key "dict" (if (string? id) (Long/valueOf id) id))))

(defn delete-dict [id]
  (log/info "delete-dict" [id])
  (let [k (create-key "dict" (if (string? id) (Long/valueOf id) id))]
    (with-transaction
      (ds-delete k)
      (ds-delete (map get-key (query-seq (.setKeysOnly (query "tag" k)))))
      (ds-delete (map get-key (query-seq (.setKeysOnly (query "version" k))))))))
