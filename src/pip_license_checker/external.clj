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

(ns pip-license-checker.external
  "External files adapters"
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [pip-license-checker.data :as d]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]))

(def regex-version-separator #"(@|:)")

(def default-options {:skip-header true :skip-footer true})

(def format-csv "csv")
(def format-cocoapods "cocoapods")
(def format-gradle "gradle")
(def format-edn "edn")

(def formats
  (sorted-set
   format-csv
   format-cocoapods
   format-gradle
   format-edn))

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

(defn package-name->requirement
  "Format package string into requirement map"
  [package]
  (let [split (if package (str/split package regex-version-separator) [])
        splitted? (> (count split) 1)
        version (if splitted? (last split) nil)
        name (if splitted? (str/join (butlast split)) (first split))]
    (d/map->Requirement {:name name :version version})))

(defn license-name->map
  "Format license name into a license record"
  [name]
  (license/license-with-type name))

(defn external-obj->dep
  "Format object parsed from external file into dependency object"
  [{:keys [package license]}]
  (d/map->Dependency
   {:requirement (package-name->requirement package)
    :license (license-name->map license)
    :error (:error license)}))

;; External file format to function

(defn get-extenal-package-parse-fn
  "Get parse function for given external file format"
  [{:keys [external-format]}]
  (cond
    (= external-format format-cocoapods) file/cocoapods-plist->data
    (= external-format format-gradle) file/gradle-json->data
    (= external-format format-edn) file/edn->data
    :else file/csv->data))

;; Entrypoint

(defn get-parsed-deps
  "Apply filters and get verdicts for all deps"
  [external options]
  (->> external
       (file/get-all-extenal-files-content
        (get-extenal-package-parse-fn options)
        (:external-options options))
       (map external-obj->dep)
       (filters/remove-requiment-maps-user-rules (:exclude options))))
