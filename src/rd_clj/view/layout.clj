(ns rd-clj.view.layout
  (:use [hiccup.core :only [html h]]
        [ring.util.response :only [redirect]]
        [am.ik.clj-gae-ds.core]
        [am.ik.clj-gae-users.core]
        [rd-clj.utils]
        [rd-clj.view.error]))

(defmacro default-layout [{title :title, more-head :more-head} & body]
  `(try 
    (layout-template {:title ~title, :more-head ~more-head} ~@body)
    (catch Throwable e#
      (let [handled# (handle-error e#)]
        (layout-template {:title "Error!!"} handled#)))))

(defmacro highlight-layout [{title :title, more-head :more-head} & body]
  `(default-layout {:title ~title, 
                    :more-head ~(conj 
                                 more-head
                                 [:script {:type "text/javascript"} 
                                  "hljs.tabReplace = '    ';"
                                  "hljs.initHighlightingOnLoad();"]
                                 [:script {:type "text/javascript" :src "/highlight/languages/java.js"}]
                                 [:script {:type "text/javascript" :src "/highlight/languages/bash.js"}]
                                 [:script {:type "text/javascript" :src "/highlight/languages/lisp.js"}]
                                 [:script {:type "text/javascript" :src "/highlight/highlight.js"}]
                                 [:link {:rel "stylesheet" :href "/highlight/styles/github.css"}])}
     ~@body))

(defmacro wmd-layout [{title :title, more-head :more-head} & body]
  `(highlight-layout {:title ~title, 
                      :more-head ~(conj
                                   more-head
                                   [:script {:type "text/javascript" :src "/wmd/wmd.js"}]
                                   [:script {:type "text/javascript"}
                                    "wmd_options = {output: 'Markdown'};"])}
                     ~@body))
