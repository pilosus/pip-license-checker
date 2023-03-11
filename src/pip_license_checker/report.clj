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

(ns pip-license-checker.report
  "Formatting and printing a report"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clojure.data.csv :as csv]
   [clojure.string :as str]
   [pip-license-checker.data :as d]))

(def items-header ["Dependency" "License Name" "License Type" "Misc"])
(def totals-header ["License Type" "Found"])
(def report-headers
  (d/map->ReportHeader
   {:items items-header
    :totals totals-header}))

(def report-formatter "%-35s %-55s %-20s %-40s")
(def printf-specifier-regex #"\%[0 #+-]?[0-9*]*\.?\d*[hl]{0,2}[jztL]?[diuoxXeEfgGaAcpsSn%]")
(def format-stdout "stdout")
(def format-json "json")
(def format-json-pretty "json-pretty")
(def format-csv "csv")

(def formats
  (sorted-set
   format-stdout
   format-json
   format-json-pretty
   format-csv))

(def invalid-format
  (format "Invalid external format. Use one of: %s"
          (str/join ", " formats)))

(defn valid-format?
  "Return true if format string is valid, false otherwise"
  [format]
  (contains? formats format))

(defn valid-formatter?
  "Check if printf-style formatter string is valid"
  [s]
  (try
    (boolean (apply format s items-header))
    (catch Exception _ false)))

(def invalid-formatter
  (format
   (str "Invalid formatter string. "
        "Expected a printf-style formatter to cover %s columns of string data, "
        "e.g. '%s'")
   (count items-header)
   report-formatter))

(defn get-totals-fmt
  "Get a substring of printf-style formatter string"
  ([] (get-totals-fmt report-formatter 2))
  ([s] (get-totals-fmt s 2))
  ([s n]
   (let [delim
         (-> s
             (str/split printf-specifier-regex)
             ;; first is the empty string
             rest
             ;; assume all specifiers separated with the same delimiter
             first)
         split-pattern (re-pattern delim)
         parts (str/split s split-pattern)
         fmt (->> parts (take n) (str/join delim))]
     fmt)))

(defn get-fmt
  "Get printf-style format string for given options and entity (:totals or :items)"
  [options entity]
  (let [{:keys [formatter] :or {formatter report-formatter}} options]
    (if (= entity :totals)
      (get-totals-fmt formatter)
      formatter)))

(defn get-items
  "Get a list of dependency fields ready printing"
  [dep]
  (let [{:keys [dependency license misc]} dep
        {dep-name :name dep-version :version} dependency
        {license-name :name license-type :type} license
        package (str/join ":" (remove str/blank? [dep-name dep-version]))
        license-misc (or misc "")]
    [package license-name license-type license-misc]))

(defn print-line
  [items formatter]
  (println (apply format formatter items)))

(defn print-report
  "Default report printer to standard output"
  [report options]
  (let [{headers-opt :headers
         totals-opt :totals
         totals-only-opt :totals-only} options
        {:keys [items totals headers]} report
        show-totals (or totals-opt totals-only-opt)]

    (when (not totals-only-opt)
      (when headers-opt
        (print-line (:items headers) (get-fmt options :items)))

      (doseq [i items]
        (print-line (get-items i) (get-fmt options :items)))

      (when totals-opt
        (println)))

    (when show-totals
      (when headers-opt
        (print-line (:totals headers) (get-fmt options :totals)))

      (doseq [t totals]
        (print-line t (get-fmt options :totals))))

    ;; return report for pipe to work properly
    report))

(defn print-line-csv
  [items]
  (csv/write-csv *out* items :quote? (constantly true))
  (flush))

(defn print-csv
  "CSV report printer to standard output"
  [report options]
  (let [{headers-opt :headers
         totals-opt :totals
         totals-only-opt :totals-only} options
        {:keys [items totals headers]} report
        show-totals (or totals-opt totals-only-opt)]

    (when (not totals-only-opt)
      (when headers-opt
        (print-line-csv [(:items headers)]))

      (print-line-csv (map get-items items))

      (when totals-opt
        (print-line-csv [[]])))

    (when show-totals
      (when headers-opt
        (print-line-csv [(:totals headers)]))

      (print-line-csv (vec totals)))

    ;; return report for pipe to work properly
    report))

(defmulti format-report
  "Format report and print to stdout"
  (fn [_ options]
    (get options :report-format)))

(defmethod format-report :default [report options]
  (print-report report options))

(defmethod format-report "json" [report _]
  (let [result (json/generate-string report)]
    (println result)
    result))

(defmethod format-report "json-pretty" [report _]
  (let [result (json/generate-string report {:pretty true})]
    (println result)
    result))

(defmethod format-report "csv" [report options]
  (print-csv report options))
