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
(def trim-whitespace-regex #"\s+")
(def trim-comment-regex #"#.*")
(def skip-line-regex #"^(?!(?:https?:\/\/|#|-)).*")
(def split-dep-regex #"(===|==|>=|<=|~=|!=)|(<|>)")
(def split-multiple-versions #"(\s+|,)")
(def split-extra-deps-regex #"\[")
(def split-after-version-regex #"(\.\*|;|@)")

;;
;; Parse requirements file line
;;

(defn path->lines
  "Return a vector of file lines"
  [path]
  (with-open [rdr (io/reader path)]
    (vec (for [line (line-seq rdr)] line))))


(defn filtered-lines
  "Filter vec of strings by predefined rules and an optional pattern
  (filtered-lines lines)  ;; only predefined filters
  (filtered-lines lines \"(?!aio).*\")  ;; exclude lines with aio.*"
  ([lines]
   (filter #(re-matches skip-line-regex %) lines))
  ([lines pattern-string]
   (let [pattern (re-pattern pattern-string)
         filtered (filter #(re-matches pattern %) lines)]
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
  (let [trimmed (str/replace line trim-whitespace-regex "")
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
  [path action]
  (if (not (.exists (io/file path)))
    (print-file-exception-message path)
    (let [lines (path->lines path)
          filtered (filtered-lines lines)
          deps (map line->dep filtered)]
      (doseq [[name version] deps]
        (println (action name version))))))
