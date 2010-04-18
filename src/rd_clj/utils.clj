(ns rd-clj.utils
  (:use [hiccup.core :only [html h]]
        [ring.util.response :only [redirect]]
        [clojure.contrib.str-utils :only [str-join]]
        [rd-clj.validator.validator]
        [am.ik.clj-gae-ds.core]
        [am.ik.clj-gae-users.core]))

(defn get-h [entity key]
  (let [v (get-prop entity key)]
    (if (instance? java.util.List v) (map h v) (h v))))

(defn adhoc-esc [#^String str]
  (-> (java.util.regex.Pattern/compile "script" java.util.regex.Pattern/CASE_INSENSITIVE)
      (.matcher str)
      (.replaceAll "ｓcript")))

(defn url-enc [str]
  (if str (java.net.URLEncoder/encode str)))

(defn url-dec [str]
  (if str (java.net.URLDecoder/decode str)))

(defn date [#^java.util.Date d]
  (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm:ss") d))

(defn params->query-string [params]
  (str-join \& (map #(str (key %) \= (url-enc (val %))) params)))

(def #^{:private true}
     +view-base+ ".view.")

(defn ns->route [ns]
  (let [view-base-pos (.indexOf (str ns) +view-base+)
        ns-str (.substring (str ns) (+ view-base-pos (count +view-base+)))]
    (str "/" ns-str)))

(defmacro fn->route-apply [route-fn apply-fn]
  `(let [meta# (meta (var ~route-fn))
         use-ns# (not (:not-use-ns meta#))
         last-path# (str (or (:route meta#) (:name meta#)))
         route-path# (str (if use-ns# (ns->route (:ns meta#))) (if-not (empty? last-path#) "/") last-path#)
         params# (:params meta#)]
     (if params# (apply str route-path# (map ~apply-fn params#)) route-path#)))

(defmacro fn->route [route-fn]
  `(fn->route-apply ~route-fn (fn [x#] (str "/" x#))))

(defmacro fn->path-fmt [route-fn]
  `(fn->route-apply ~route-fn (fn [x#] (str "/%s"))))

(defn 
  #^{:not-use-ns true}
  login []
  (redirect (create-login-url "/")))

(defn 
  #^{:not-use-ns true}
  logout []
  (redirect (create-logout-url "/")))

(defmacro layout-template [{title :title, more-head :more-head} & body]
  (let [default-ttl "逆引きClojure"
        ttl (if title `(str ~title " - " ~default-ttl) default-ttl)
        css "/css/style.css"]
    `(html [:html 
            [:head 
             [:title ~ttl]
             [:link {:rel "stylesheet" :type "text/css" :href ~css :media "screen"}]
             ~@more-head]
            [:body 
             [:div {:id "wrap"}
              [:div {:id "header"}
               [:h1 [:a {:href "/"} ~default-ttl]]]
              [:div {:id "content"}
               [:div  {:class "left"}
                ~@body]
               [:div  {:class "right"}
                [:h2 "ようこそ " 
                 (if (get-current-user) 
                   (.getNickname (get-current-user))
                   "Guest") "さん"]
                [:ul
                 (if (get-current-user) 
                   [:li [:a {:href (fn->path-fmt logout)} "LOGOUT"]]
                   [:li [:a {:href (fn->path-fmt login)} "LOGIN"]])]
                ]
               [:div {:style "clear:both;"}]
               [:div {:id "footer"} 
                "&copy; clojure-users.org "
                [:img {:src "/images/appengine-noborder-120x30.gif"}]
                " "
                [:img {:src "/images/clojure-100x30.gif"}]
                ]
               ]]]])))

(defn apply-auth
  ([fn]
     (if (user-logged-in?)
       (fn)
       ;; atode url wo tukuru from ..
       (redirect (create-login-url "/"))))
  ([fn req]
     (if (user-logged-in?)
       (fn req)
       ;; atode url wo tukuru ..
       (redirect (create-login-url "/")))))

(defn validation-error [req messages submit-key]
  (layout-template {:title "Validation Error!!"}
    [:h2 "Validation Error!!"]
    [:ul {:style "color:red;"}
     (for [m messages]
       [:li m])]
    [:hr]
    [:form
     {:action (:uri req) :method :post}
     (for [p (dissoc (:params req) submit-key)]
       [:input {:type :hidden :name (key p) :value (val p)}])
     [:input {:type :submit :value "Back"}]]))

(defmacro with-validation [req validator-info & body]
  `(if (get-in ~req [:params ~(:submit validator-info)])
     (let [messages#
           ~(let [target (:target validator-info)]
              `(->> []
                    ~@(mapcat identity 
                              (for [[name vals] target]
                                (for [[type attr] vals]
                                  `(validate ~type ~name (get-in ~req [:params ~name]) ~attr))))))]
       (if (empty? messages#)
         (do ~@body)
         (validation-error ~req messages# ~(:submit validator-info))))
     (do
       ;; not submitted
       ~@body)))

(comment 
  ;; validation sample

  (with-validation req {:submit "do" 
                        :target {"title" [[:require]], 
                                 "content" [[:require]
                                            [:string-length {:gt 100}]]}}
    (println req))
  ;; is expanded to
  (if (get-in req [:params "do"])
    (let [messages (->> [] (validate :require "title" (get-in req [:params "title"]) nil)
                           (validate :require "content" (get-in req [:params "content"]) nil)
                           (validate :string-length "content" (get-in req [:params "content"]) {:gt 100}))]
      (if (empty? messages)
          (do (println req))
          (validation-error req messages "do")))
    (do (println req)))
  )

(defmacro defroutes+ [app & routes]
  `(~'defroutes ~app
    ~@(for [[method route-fn] routes]
        (let [meta# (eval `(meta (var ~route-fn)))
              arg# (first (:arglists meta#))
              auth# (:auth meta#)
              validate# (:validate meta#)
              route# (eval `(fn->route ~route-fn))
              body# (if auth# 
                      (if (empty? arg#) `(apply-auth ~route-fn) `(apply-auth ~route-fn ~'req))
                      (if (empty? arg#) `(~route-fn) `(~route-fn ~'req)))
              validated# (if validate# `(with-validation ~'req ~validate# ~body#) body#)]
          `(~method ~route# ~'req ~validated#)))))
