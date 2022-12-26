;; Copyright © 2020-2022 Vitaly Samigullin
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
   [clojure.string :as str]
   [pip-license-checker.data :as d]))

(def items-header ["Dependency" "License Name" "License Type" "Misc"])
(def totals-header ["License Type" "Found"])
(def report-headers
  (d/map->ReportHeader
   {:items items-header
    :totals totals-header}))

(def report-formatter "%-35s %-55s %-20s")
(def verbose-formatter "%-40s")

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
   (let [parts (str/split s #"\s+")
         fmt (->> parts (take n) (str/join " "))]
     fmt)))

(defn get-fmt
  "Get printf-style format string for given options and entity (:totals or :items)"
  [options entity]
  (let [{:keys [formatter] :or {formatter report-formatter}} options
        fmt (if (:verbose options)
              (format "%s %s" formatter verbose-formatter)
              formatter)
        fmt' (if (= entity :totals) (get-totals-fmt fmt) fmt)]
    fmt'))

(defn get-items
  "Get a list of dependency fields ready printing"
  [dep]
  (let [{:keys [dependency license error]} dep
        {dep-name :name dep-version :version} dependency
        {license-name :name license-type :type} license
        package (str/join ":" (remove str/blank? [dep-name dep-version]))
        misc (or error "")]
    [package license-name license-type misc]))

(defn print-line
  [items formatter]
  (println (apply format formatter items)))

(defn print-report
  "Print report to standard output"
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
