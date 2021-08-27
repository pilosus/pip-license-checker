(ns pip-license-checker.external
  "External files adapters"
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]
   [cocoapods-acknowledgements-licenses.core :refer [plist->data]]))

(def prefetched-correctly true)
(def regex-version-separator #"(@|:)")

(def default-options {:skip-header true :skip-footer true})

(def format-csv "csv")
(def format-cocoapods "cocoapods")

(def formats
  (sorted-set
   format-csv
   format-cocoapods))

(def invalid-format
  (format "Invalid external format. Use one of: %s"
          (str/join ", " formats)))

(defn is-format-valid?
  "Return true if format string is valid, false otherwise"
  [format]
  (contains? formats format))

;; EDN string to Clojure data structure


(defn opts-str->map
  "Parse options string in EDN format into Clojure map"
  [options-str]
  (edn/read-string options-str))


;; Formatting and extensing package names


(defn package-name->requirement-map
  "Format package string into requirement map"
  [package]
  (let [split (if package (str/split package regex-version-separator) [])
        splitted? (> (count split) 1)
        version (if splitted? (last split) nil)
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

;; External file format to function

(defn get-extenal-package-parse-fn
  "Get parse function for given external file format"
  [{:keys [external-format]}]
  (cond
    (= external-format format-cocoapods) plist->data
    :else file/csv->data))

;; Entrypoint

(defn get-parsed-requiements
  "Apply filters and get verdicts for all requirements"
  [external options]
  (let [exclude-pattern (:exclude options)
        external-options (:external-options options)
        parse-fn (get-extenal-package-parse-fn options)
        packages (file/get-all-extenal-files-content parse-fn external external-options)

        requirements (map external-obj->requirement packages)
        result (filters/remove-requiment-maps-user-rules exclude-pattern requirements)]
    result))
