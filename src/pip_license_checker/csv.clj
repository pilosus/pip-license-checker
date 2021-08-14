(ns pip-license-checker.csv
  "Process packages with prefetched licenses from CSV file"
  (:gen-class)
  (:require
   [clojure.string :as str]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]))

(def prefetched-correctly true)

(def regex-version-separator #"(@|:)")

(defn package-name->requirement-map
  "Format package string into requirement map"
  [package]
  (let [split (if package (str/split package regex-version-separator) [])
        splitted? (> (count split) 1)
        version (if splitted? (last split) "")
        name (if splitted? (str/join (butlast split)) (first split))]
    {:name name :version version}))

(defn license-name->map
  "Format license name into license map"
  [license]
  {:name license :type (license/name->type license)})

(defn external-obj->requirement
  "Format object parsed from external file into requirement object"
  [{:keys [package license]}]
  {:ok? prefetched-correctly
   :requirement (package-name->requirement-map package)
   :license (license-name->map license)})

(defn get-parsed-requiements
  "Apply filters and get verdicts for all requirements"
  [external options]
  (let [exclude-pattern (:exclude options)
        packages (file/get-csv-lines external options)
        requirements (map external-obj->requirement packages)
        result (filters/remove-requiment-maps-user-rules exclude-pattern requirements)]
    result))
