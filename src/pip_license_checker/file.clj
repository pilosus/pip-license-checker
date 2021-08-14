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

(defn take-csv-columns
  [columns indices]
  (for [idx indices]
    (nth columns idx)))

(defn csv->lines
  "Read CSV file in format: package-name,license-name[,...] into a map"
  [path options]
  (with-open [reader (io/reader path)]
    (let [lines (csv/read-csv reader)
          with-headers (:external-csv-headers options)
          lines-to-skip (if with-headers 1 0)
          data (drop lines-to-skip lines)
          selected-columns (mapv #(take-csv-columns % csv-column-indecies-to-read) data)
          result (map zipmap (repeat csv-header) selected-columns)]
      result)))

(defn get-csv-lines
  "Concatenates all parsed CSV files"
  [paths with-headers]
  (apply concat (for [path paths] (csv->lines path with-headers))))
