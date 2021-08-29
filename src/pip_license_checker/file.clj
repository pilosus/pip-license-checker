(ns pip-license-checker.file
  "Reading and parsing files"
  (:gen-class)
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]))

(defn exists?
  "Return true if file exists"
  [path]
  (.exists (io/file path)))

;; requirements.txt files

(defn path->lines
  "Return a vector of file lines
  Vector used instead of lazy seq to handle open/close file using with-open"
  [path]
  (with-open [reader (io/reader path)]
    (vec (for [line (line-seq reader)] line))))

(defn get-requirement-lines
  "Get a sequence of lines from all requirement files"
  [requirements]
  (apply concat (for [path requirements] (path->lines path))))

;; CSV files

(def csv-header [:package :license])
(def csv-out-of-range-column-index nil)

(defn take-csv-columns
  [columns indices]
  (for [idx indices]
    (nth columns idx csv-out-of-range-column-index)))

(defn csv->lines
  "Read CSV file into vector of lines"
  [path]
  (with-open [reader (io/reader path)]
    (let [lines (csv/read-csv reader)
          result (vec lines)]
      result)))

(defn csv->data
  "Read CSV file into a map"
  [path external-options]
  (let [{:keys [skip-header package-column-index license-column-index]
         :or {skip-header true package-column-index 0 license-column-index 1}} external-options
        csv-column-indecies-to-read [package-column-index license-column-index]
        lines (csv->lines path)
        lines-to-skip (if skip-header 1 0)
        data (drop lines-to-skip lines)
        selected-columns (map #(take-csv-columns % csv-column-indecies-to-read) data)
        result (map zipmap (repeat csv-header) selected-columns)]
    result))

;; Universal, used with external files adapters

(defn get-all-extenal-files-content
  "Concatenates all parsed external files"
  [parse-fn paths external-options]
  (apply concat (for [path paths] (parse-fn path external-options))))
