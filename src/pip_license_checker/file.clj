(ns pip-license-checker.file
  "Reading and parsing requirements.txt files with pip dependencies"
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))

;;
;; Const
;;

;; See PyPI specs
;; https://pip.pypa.io/en/stable/reference/pip_install/#requirements-file-format
;; https://www.python.org/dev/peps/pep-0440/#version-specifiers
(def trim-spaces-and-comments-regex #"(?:\s+)|(?:#.*)")
(def trim-comment-regex #"#.*")
(def skip-line-regex #"^(?:https?:\/\/|#|-).*")
(def skip-blank-line-regex #"^\s*$")
(def split-dep-regex #"(===|==|>=|<=|~=|!=)|(<|>)")
(def split-multiple-versions #"(\s+|,)")
(def split-extra-deps-regex #"\[")
(def split-after-version-regex #"(\.\*|;|@)")

;;
;; Parse requirements file line
;;

(defn path->lines
  "Return a vector of file lines
  Vector used instead of lazy seq to handle open/close file using with-open"
  [path]
  (with-open [rdr (io/reader path)]
    (vec (for [line (line-seq rdr)] line))))


(defn filtered-lines
  "Filter out vec of strings by predefined rules and an optional pattern"
  ([lines]
   (->>
    (remove #(re-matches skip-blank-line-regex %) lines)
    (remove #(re-matches skip-line-regex %))))
  ([lines pattern-string]
   (let [pattern (re-pattern pattern-string)
         filtered (remove #(re-matches pattern %) lines)]
     (filtered-lines filtered))))


(defn trimmed-dep-name
  "Remove unused characters from dependency name"
  [name]
  (first (str/split name split-extra-deps-regex)))


(defn trimmed-dep-version
  "Remove unused characters from dependency version"
  [version]
  (if (some? version)
    (let [trimmed (str/replace version trim-comment-regex "")]
      (first
       (str/split
        (first (str/split trimmed split-after-version-regex))
        split-multiple-versions)))))


(defn line->dep
  "Parse line into dependency record"
  [line]
  (let [trimmed (str/replace line trim-spaces-and-comments-regex "")
        [name version] (str/split trimmed split-dep-regex)]
    (vec (filter some? [(trimmed-dep-name name) (trimmed-dep-version version)]))))


;;
;; Integration (side effects)
;;




(defn print-file-exception-message
  "Print error message for file path"
  [path]
  (println "No such file: " path))


(defn print-file
  "Apply function to parsed deps, print results"
  [path action pattern]
  (if (not (.exists (io/file path)))
    (print-file-exception-message path)
    (let [lines (path->lines path)
          filtered (if (some? pattern)
                     (filtered-lines lines pattern)
                     (filtered-lines lines))
          deps (map line->dep filtered)]
      (doseq [[name version] deps]
        (println (action name version))))))


;;
;;
;;

(defn get-requirement-lines
  "Get a sequence of lines from all requirement files"
  [requirements]
  (apply concat (for [path requirements] (path->lines path))))
