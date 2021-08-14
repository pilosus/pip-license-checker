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


(def csv-column-indecies-to-read [0 1])
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
  "Read CSV file in format: package-name,license-name[,...] into a map"
  [path options]
  (let [lines (csv->lines path)
        with-headers (:external-csv-headers options)
        lines-to-skip (if with-headers 1 0)
        data (drop lines-to-skip lines)
        selected-columns (map #(take-csv-columns % csv-column-indecies-to-read) data)
        result (map zipmap (repeat csv-header) selected-columns)]
    result))

(defn get-csv-lines
  "Concatenates all parsed CSV files"
  [paths with-headers]
  (apply concat (for [path paths] (csv->data path with-headers))))
