(ns rd-clj.view.tag
  (:use [hiccup.core :only [html h]]
        [rd-clj.utils]
        [rd-clj.view.layout]
        [rd-clj.daccess.dict])
  (:refer-clojure :exclude [list]))

(defn list-dicts [req]
  (let [tag (url-dec (get-in req [:params :tag]))]
    (default-layout {} 
      [:h2 "Tagged by " (h tag) " ..."]
      (show-dicts (tagname->dicts tag)))))
