(ns rd-clj.validator.validator)

(defmulti validate (fn [x & _] x))

(defmethod validate :require [type name value attr messages]
  (if (empty? value) (conj messages (str name " is empty!")) messages))

(defmethod validate :string-length [type name value attr messages]
  (let [lt (:lt attr)
        gt (:gt attr)
        lte (:lte attr)
        gte (:gte attr)
        len (count value)]        
    (cond (and (integer? lt) (not (< len lt))) (conj messages (str "the length of " name " is not less than " lt "!"))
          (and (integer? gt) (not (> len gt))) (conj messages (str "the length of " name " is not greater than " gt "!"))
          (and (integer? lte) (not (<= len lte))) (conj messages (str "the length of " name " is not equals or not less than " lte "!"))
          (and (integer? gte) (not (>= len gte))) (conj messages (str "the length of " name " is not equals or not greater than " gte "!"))
          :default messages)))

(defmethod validate :regex [type name value attr messages]
  (let [regex (:regex attr)
        re (if (string? regex) (re-pattern regex) regex)]
    (if (and regex (not (re-matches re value)))
      (conj messages (str name " is not regular")))))

(defmethod validate :default [type name value attr messages]
  messages)