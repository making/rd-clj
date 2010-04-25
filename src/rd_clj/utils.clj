(ns rd-clj.utils
  (:use [hiccup.core :only [h]]
        [clojure.contrib.str-utils :only [str-join]]
        [am.ik.clj-gae-ds.core])
  (:import [com.google.appengine.api.memcache 
            MemcacheService MemcacheServiceFactory 
            Expiration MemcacheService$SetPolicy]))

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

(defn #^MemcacheService get-mc-service 
  ([namespace]
     (let [#^MemcacheService result (MemcacheServiceFactory/getMemcacheService)]
       (if namespace (.setNamespace result namespace))
       result))
  ([]
     (get-mc-service nil)))

(defn mc-put 
  ([namespace key value expiration set-policy]
     (let [mc (get-mc-service namespace)]
       (.put mc key value expiration set-policy)))
  ([namespace key value expiration]
     (mc-put namespace key value expiration MemcacheService$SetPolicy/SET_ALWAYS))
  ([namespace key value]
     (mc-put namespace key value (Expiration/byDeltaSeconds 86400)))) ; 1 day default

(defn mc-get
  ([namespace key default-value]
     (let [result (.get (get-mc-service namespace) key)]
       (or result default-value)))
  ([namespace key]
     (mc-get namespace key nil)))

(defn mc-delete [namespace key]
  (.delete (get-mc-service namespace) key))

(defn mc-clear []
  (.clearAll (get-mc-service)))  

(defn mc-statistics []
  (let [mc (get-mc-service)]
    (.getStatistics mc)))