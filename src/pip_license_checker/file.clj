(ns pip-license-checker.file
  "Reading and parsing requirements.txt files with pip dependencies"
  (:gen-class)
  (:require
   [clojure.string :as str]))

;;
;; Const
;;


;; https://pip.pypa.io/en/stable/reference/pip_install/#requirements-file-format
;; https://www.python.org/dev/peps/pep-0440/#version-specifiers
(def skip-line-regex #"^(?:https?:\/\/|#|-).*")
(def split-dep-regex #"(===|==|>=|<=|~=|!=)|(<|>)")
(def split-multiple-versions #"(\s+|,)")
(def split-extra-deps-regex #"\[")
(def split-after-version-regex #"(\.\*|;|@)")

;;
;; Parse string
;;

(defn trim-dep-name
  "Remove unused characters from dependency name"
  [name]
  (let [trimmed (str/trim name)]
    (first (str/split trimmed split-extra-deps-regex))))


(defn trim-dep-version
  "Remove unused characters from dependency version"
  [version]
  (if (some? version)
    (let [trimmed (str/trim version)]
      (first
       (str/split
        (first (str/split trimmed split-after-version-regex))
        split-multiple-versions)))))


(defn line->dep
  "Parse line into dependency record"
  [line]
  (let [trimmed (str/trim line)
        line-to-skip (re-matches skip-line-regex trimmed)]
    (if (nil? line-to-skip)
     (let [[name version] (str/split trimmed split-dep-regex)]
      (vec (filter some? [(trim-dep-name name) (trim-dep-version version)]))))))
