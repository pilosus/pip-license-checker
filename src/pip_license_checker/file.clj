;; Copyright © 2020-2023 Vitaly Samigullin
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
   [cheshire.core :as json]
   [clojure.data.csv :as csv]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.github.bdesham.clj-plist :refer [parse-plist]]))

(defn exists?
  "Return true if file exists"
  [path]
  (.exists (io/file path)))

(defn path->string
  "Return a string with the file contents"
  [path]
  (slurp path))

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

(defn- format-edn-package-name
  "Apply options to format EDN package name"
  [package options]
  (let [{:keys [fully-qualified-names]
         :or {fully-qualified-names true}} options
        ;; package is a symbol
        package-str (str package)
        result
        (cond
          (and (not fully-qualified-names)
               (not-empty package-str))
          (last (str/split package-str #"/"))
          :else (not-empty package-str))]
    result))

(defn edn-item->data-item
  "Format edn item into a package data map"
  [[[package version] license] options]
  (let [package-formatted (format-edn-package-name package options)
        version-formatted (not-empty version)
        parts (cond
                (and package-formatted version-formatted)
                [package-formatted version-formatted]
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

;; Cocoapods

(def cocoapods-key-specifiers "PreferenceSpecifiers")
(def cocoapods-key-package-name "Title")
(def cocoapods-key-license-name "License")

(defn- cocoapods-plist-item->map
  "Convert plist item map to proper format"
  [item]
  (let [package (get item cocoapods-key-package-name)
        license (get item cocoapods-key-license-name)]
    {:package package :license license}))

(defn cocoapods-plist->data
  "Parse Plist into vector of {:package PACKAGE :license LICENSE} maps"
  [path options]
  (let [{:keys [skip-header skip-footer] :or {skip-header true skip-footer true}} options
        parsed-plist (parse-plist path)
        deps (get parsed-plist cocoapods-key-specifiers)
        formatted (map cocoapods-plist-item->map deps)
        formatted' (if skip-header (rest formatted) formatted)
        formatted'' (if skip-footer (butlast formatted') formatted')]
    formatted''))

;; Gradle JSON

(def gradle-json-key-dependency "dependency")
(def gradle-json-key-licenses "licenses")
(def gradle-json-key-license "license")
(def gradle-json-key-project "project")
(def gradle-json-key-version "version")

(defn gradle-json-license-vec->license
  "Convert a vector of licenses into nilable license string"
  [license-vec]
  (let [license-names (map #(get % gradle-json-key-license) license-vec)
        license-str (str/join ", " license-names)
        result (not-empty license-str)]
    result))

(defn gradle-json-dependency->package
  "Get package name nilable string from a map"
  [dependency options]
  (let [{:keys [fully-qualified-names] :or {fully-qualified-names true}} options
        project (not-empty (get dependency gradle-json-key-project))
        version (not-empty (get dependency gradle-json-key-version))
        parts
        (cond
          (and project version) [project version]
          project [project]
          :else nil)
        package
        (if fully-qualified-names
          (get dependency gradle-json-key-dependency)
          (not-empty (str/join ":" parts)))]
    package))

(defn gradle-json-dependency->map
  "Convert dependency item from the parsed JSON into a map"
  [dependency options]
  (let [package-str (gradle-json-dependency->package dependency options)
        license-vec (get dependency gradle-json-key-licenses)
        license-str (gradle-json-license-vec->license license-vec)]
    {:package package-str :license license-str}))

(defn gradle-json->data
  "Parse gradle-license-plugin JSON into vector of {:package PACKAGE :license LICENSE} maps"
  #_:clj-kondo/ignore
  [path options]
  (let [content (path->string path)
        dependencies (json/parse-string content)
        result (map #(gradle-json-dependency->map % options) dependencies)]
    result))

;; Universal, used with external files adapters

(defn get-all-extenal-files-content
  "Concatenates all parsed external files"
  [parse-fn external-options paths]
  (apply concat (for [path paths] (parse-fn path external-options))))
