(ns rd-clj.utils
  (:use [hiccup.core :only [h]]
        [am.ik.clj-gae-ds.core]))

(defn get-h [entity key]
  (let [v (get-prop entity key)]
    (if (instance? java.util.List v) (map h v) (h v))))

(defn url-enc [str]
  (java.net.URLEncoder/encode str))

(defn url-dec [str]
  (java.net.URLDecoder/decode str))
