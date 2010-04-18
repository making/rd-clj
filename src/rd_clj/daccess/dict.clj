(ns rd-clj.daccess.dict
  (:use [am.ik.clj-gae-ds.core]
        [rd-clj.utils]
        [rd-clj.view.layout]))

(def #^{:private true}
     +max-query-num+ 500)

(defn tagname->dicts 
  ([tagname num]
     (vals (ds-get (map get-parent (take num (query-seq (-> (q "tag") (flt :name = tagname))))))))
  ([tagname]
     (tagname->dicts tagname +max-query-num+)))

(defn dict->tags 
  ([dict num]
     (vals (ds-get (map get-key (take num (query-seq (q "tag" (get-key dict))))))))
  ([dict]
     (dict->tags dict +max-query-num+)))

(defn create-dict [entity]
  (with-transaction 
    (let [k (ds-put entity)]
      (ds-put (map #(map-entity "tag" :name % :parent k) (get-prop entity :tags))))))

(defn update-dict [dict new-tags update-date?]
  (with-transaction
    (ds-delete (map get-key (dict->tags dict))) ; delete old tags
    (set-prop dict :tags new-tags) ; retag
    (if update-date? (set-prop dict :updated (java.util.Date.)))
    (let [k (ds-put dict)]
      (ds-put (map #(map-entity "tag" :name % :parent k) new-tags)))))

(defn get-dicts 
  ([num]
     (take num (query-seq (-> (q "dict") (srt :updated :desc)))))
  ([]
     (get-dicts +max-query-num+)))

(defn get-dict [id]
  (ds-get (create-key "dict" (if (string? id) (Long/valueOf id) id))))

(defn delete-dict [id]
  (let [k (create-key "dict" (if (string? id) (Long/valueOf id) id))]
    (with-transaction
      (ds-delete k)
      (ds-delete (map get-key (query-seq (.setKeysOnly (query "tag" k))))))))
