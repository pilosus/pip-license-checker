(ns pip-license-checker.filters
  "Filters for requirements"
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   ;;[clojure.spec.test.alpha :refer [instrument]]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.version :as version]))

;; Skip line with -r /--requirement/-e etc, URLs, blank lines, comments
(def regex-skip-line-internal #"(\s*(?:https?:\/\/|#|-).*)|(^\s*$)")

(def regex-remove-whitespace #"\s*")
(def regex-remove-comment #"#.*")
(def regex-remove-modifiers #"(;|@).*")
(def regex-remove-extra #"\[.*\]")
(def regex-remove-wildcard #"\.\*")
(def regex-split-specifier-ops #"(===|==|~=|!=|>=|<=|<|>)")

(s/fdef remove-requirements-internal-rules
  :args (s/cat :requirements ::sp/requirements)
  :ret ::sp/requirements)

(defn remove-requirements-internal-rules
  "Exclude requirements from sequence according to app's internal rules"
  [requirements]
  (remove #(re-matches regex-skip-line-internal %) requirements))

(s/fdef remove-requirements-user-rules
  :args (s/cat :pattern ::sp/opt-pattern :requirements ::sp/requirements)
  :ret ::sp/requirements)

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
   (str/replace regex-remove-wildcard "")))

(s/fdef requirement->map
  :args (s/cat :requirement ::sp/requirement)
  :ret ::sp/requirement-map)

(defn requirement->map
  "Parse requirement string into map with package name and its specifiers parsed"
  [requirement]
  (let [package-name (first (str/split requirement regex-split-specifier-ops))
        specifiers-str (subs requirement (count package-name))
        specifiers-vec (version/parse-specifiers specifiers-str)
        specifiers (if (= specifiers-vec [nil]) nil specifiers-vec)
        result {:name package-name :specifiers specifiers}]
    result))

;;
;; Instrumented functions - uncomment only while testing
;;

;; (instrument `remove-requirements-internal-rules)
;; (instrument `remove-requirements-user-rules)
;; (instrument `requirement->map)
