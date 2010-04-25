(ns rd-clj.logger
  (:import [java.util.logging Logger Level]))

(defn #^Logger get-logger [name]
  (Logger/getLogger name))

(defmacro deflogger [level]
  `(defn ~level [~'name ~'message]
     (. (get-logger ~'name) ~level (str ~'name " " ~'message))))

(deflogger severe)
(deflogger warning)
(deflogger info)
(deflogger config)
(deflogger fine)
(deflogger finer)
(deflogger finest)

