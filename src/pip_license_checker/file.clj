(ns pip-license-checker.file
  "Reading and parsing requirements.txt files"
  (:gen-class)
  (:require
   [clojure.java.io :as io]))

(defn exists?
  "Return true if file exists"
  [path]
  (.exists (io/file path)))

(defn path->lines
  "Return a vector of file lines
  Vector used instead of lazy seq to handle open/close file using with-open"
  [path]
  (with-open [rdr (io/reader path)]
    (vec (for [line (line-seq rdr)] line))))

(defn get-requirement-lines
  "Get a sequence of lines from all requirement files"
  [requirements]
  (apply concat (for [path requirements] (path->lines path))))
