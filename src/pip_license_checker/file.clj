;; Copyright © 2020, 2021 Vitaly Samigullin
;;
;; This program and the accompanying materials are made available under the
;; terms of the Eclipse Public License 2.0 which is available at
;; http://www.eclipse.org/legal/epl-2.0.
;;
;; This Source Code may also be made available under the following Secondary
;; Licenses when the conditions for such availability set forth in the Eclipse
;; Public License, v. 2.0 are satisfied: GNU General Public License as published by
;; the Free Software Foundation, either version 2 of the License, or (at your
;; option) any later version, with the GNU Classpath Exception which is available
;; at https://www.gnu.org/software/classpath/license.html.
;;
;; SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

(ns pip-license-checker.file
  "Reading and parsing files"
  (:gen-class)
  (:require
   [clojure.data.csv :as csv]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

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


;; EDN files


(defn path->string
  "Return a string with the file contents"
  [path]
  (slurp path))

(defn- format-edn-package-name
  "Apply options to format EDN package name"
  [package options]
  (let [{:keys [fully-qualified-names]
         :or {fully-qualified-names true}} options
        ;; package is a symbol
        package-str (str package)
        result
        (cond
          (and (not fully-qualified-names) (not-empty package-str)) (last (str/split package-str #"/"))
          :else (not-empty package-str))]
    result))

(defn edn-item->data-item
  "Format edn item into a package data map"
  [[[package version] license] options]
  (let [package-formatted (format-edn-package-name package options)
        version-formatted (not-empty version)
        parts (cond (and package-formatted version-formatted) [package-formatted version-formatted]
                    package-formatted [package-formatted]
                    :else nil)
        package-with-version (not-empty (str/join ":" parts))
        result {:package package-with-version :license license}]
    result))

(defn edn->data
  "Read EDN file into Clojure data structure"
  [path external-options]
  (->> path
       path->string
       edn/read-string
       (map #(edn-item->data-item % external-options))))

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
