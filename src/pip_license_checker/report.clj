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

(ns pip-license-checker.report
  "Formatting and printing a report"
  (:gen-class)
  (:require
   [clojure.string :as str]))

(def table-header ["Requirement" "License Name" "License Type" "Misc"])
(def totals-header ["License Type" "Found"])
(def fmt-params-split-regex #"\s+")
(def fmt-params-delim " ")

(defn- take-fmt
  "Get a substring of printf-style formatter string"
  [n fmt-str]
  (let [parts
        (->
         fmt-str
         (str/split fmt-params-split-regex))
        formatter
        (->>
         parts
         (take n)
         (str/join fmt-params-delim))]
    formatter))

(defn- get-totals-formatter
  [fmt-str]
  (take-fmt (count totals-header) fmt-str))

(def table-formatter "%-35s %-55s %-20s")
(def verbose-formatter "%-40s")
(def totals-formatter (take-fmt (count totals-header) table-formatter))

(defn valid-formatter?
  "Check if printf-style formatter string is valid"
  [fmt-str]
  (try
    (boolean (apply format fmt-str table-header))
    (catch Exception _ false)))

(def invalid-formatter
  (format (str "Invalid formatter string. "
               "Expected a printf-style formatter to cover %s columns of string data, "
               "e.g. '%s'")
          (count table-header)
          table-formatter))

(defn print-license-header
  [options]
  (let [{:keys [formatter] :or {formatter table-formatter}} options
        formatter' (if (:verbose options)
                     (format "%s %s" formatter verbose-formatter)
                     formatter)]
    (println (apply format formatter' table-header))))

(defn format-license
  "Format license"
  [license-data options]
  (let [{:keys [requirement license]} license-data
        {:keys [formatter] :or {formatter table-formatter}} options
        formatter' (if (:verbose options)
                     (format "%s %s" formatter verbose-formatter)
                     formatter)
        {req-name :name req-version :version} requirement
        package (str/join ":" (remove str/blank? [req-name req-version]))
        {lic-name :name lic-type :type error :error} license
        error' (or error "")]
    (format formatter' package lic-name lic-type error')))

(defn format-total
  "Print lincese type  totals line"
  [license-type freq options]
  (let [{:keys [formatter] :or {formatter table-formatter}} options
        totals-formatter (get-totals-formatter formatter)]
    (format totals-formatter license-type freq)))

(defn print-totals-header
  [options]
  (let [{:keys [formatter] :or {formatter table-formatter}} options
        totals-formatter (get-totals-formatter formatter)]
    (println (apply format totals-formatter totals-header))))
