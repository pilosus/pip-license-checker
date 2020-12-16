(ns pip-license-checker.filters
  "Filters for requirements"
  (:gen-class)
  (:require
   [clojure.string :as str]))

;; Skip line with -r /--requirement/-e etc, URLs, blank lines, comments
(def regex-skip-line-internal #"(\s*(?:https?:\/\/|#|-).*)|(^\s*$)")

(def regex-remove-whitespace #"\s*")
(def regex-remove-comment #"#.*")
(def regex-remove-modifiers #"(;|@).*")
(def regex-remove-extra #"\[.*\]")
(def regex-remove-comma #",")
(def regex-remove-wildcard #"\.\*")

(def regex-split-specifier-equal #"(===|==)")
(def regex-split-specifier-other #"(>=|<=|~=|!=|<|>)")

(def version-latest :latest)


(defn remove-requirements-internal-rules
  "Exclude requirements from sequence according to app's internal rules"
  [requirements]
  (remove #(re-matches regex-skip-line-internal %) requirements))

(defn remove-requirements-user-rules
  "Exclude requirements from sequence according to user-defined pattern"
  [pattern requirements]
  (if pattern
    (remove #(re-matches pattern %) requirements)
    requirements))

(defn sanitize-requirement
  "Sanitize requirement line"
  [requirement]
  (->
   requirement
   (str/replace regex-remove-whitespace "")
   (str/replace regex-remove-comment "")
   (str/replace regex-remove-modifiers "")
   (str/replace regex-remove-extra "")
   (str/replace regex-remove-comma "")
   (str/replace regex-remove-wildcard "")))

(defn requirement->map
  "Parse requirement string into hash-map

  FIXME add REAL version resolving for specifiers other than `==`
  Use `releases` field in PyPI API response and PEP-440"
  [requirement]
  (let [equal-split (str/split requirement regex-split-specifier-equal)
        other-split (str/split requirement regex-split-specifier-other)]
    (if (= (count equal-split) 2)
      {:name (first equal-split) :version (second equal-split)}
      {:name (first other-split) :version version-latest})))
