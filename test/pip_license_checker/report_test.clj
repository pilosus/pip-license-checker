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

(ns pip-license-checker.report-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.report :as report]))

(def params-valid-formatter?
  [["%s %s %s" ["A" "B" "C"] true "Valid"]
   ["" ["A" "B" "C"] true "Valid too"]
   ["%-35s %-55s %-30s" ["A" "B" "C" "D"] true "Valid, excessive column ignored"]
   ["%-35s %-55s %-30s" ["A" "B"] false "Invalid, not enough data to format"]])

(deftest test-valid-formatter?
  (testing "Printing a line of license table"
    (doseq [[fmt columns expected description] params-valid-formatter?]
      (testing description
        (with-redefs
         [report/table-header columns]
          (is (= expected (report/valid-formatter? fmt))))))))

(def params-get-totals-fmt
  [[[] "%-35s %-55s" "No args"]
   [["%-35s %-55s %-30s"] "%-35s %-55s" "Only fmt string"]
   [["%-35s %-55s %-30s" 1] "%-35s" "Format string and length"]])

(deftest test-get-totals-fmt
  (testing "Get total formatter string"
    (doseq [[args expected description] params-get-totals-fmt]
      (testing description
        (is (= expected (apply report/get-totals-fmt args)))))))

(def params-get-fmt
  [[{} :items "%-35s %-55s %-20s"
    "items, no options"]
   [{:verbose true} :items "%-35s %-55s %-20s %-40s"
    "items, verbose"]
   [{:verbose true :formatter "%s %s %s"} :items "%s %s %s %-40s"
    "items, verbose, customer formatter"]
   [{:verbose true :formatter "%s %s %s"} :totals "%s %s"
    "totals, verbose, customer formatter"]
   [{:verbose false :formatter "%s %s %s"} :totals "%s %s"
    "totals, non-verbose, customer formatter"]
   [{:verbose false} :totals "%-35s %-55s"
    "totals, non-verbose, default formatter"]])

(deftest test-get-fmt
  (testing "Get formatter string"
    (doseq [[opts entity expected description] params-get-fmt]
      (testing description
        (is (= expected (report/get-fmt opts entity)))))))

(def params-get-items
  [[{:dependency {:name "basilic", :version "0.1.3.4"},
     :license
     {:name "GNU General Public License v2 or later (GPLv2+)",
      :type "StrongCopyleft"},
     :error nil}
    ["basilic:0.1.3.4" "GNU General Public License v2 or later (GPLv2+)"
     "StrongCopyleft" ""]
    "No errors"]
   [{:dependency {:name "basilic", :version nil},
     :license
     {:name "GNU General Public License v2 or later (GPLv2+)",
      :type "StrongCopyleft"},
     :error nil}
    ["basilic" "GNU General Public License v2 or later (GPLv2+)"
     "StrongCopyleft" ""]
    "Version is absent"]
   [{:dependency {:name "aiopg", :version "122.3.5"},
     :license {:name "Error", :type "Error"},
     :error "PyPI::version Not found"}
    ["aiopg:122.3.5" "Error" "Error" "PyPI::version Not found"]
    "Error"]])

(deftest test-get-items
  (testing "Get items ready for printing"
    (doseq [[dep expected description] params-get-items]
      (testing description
        (is (= expected (report/get-items dep)))))))

(def params-print-line
  [[["basilic" "GNU General Public License v2 or later (GPLv2+)"
     "StrongCopyleft" ""]
    "%s %s %s"
    "basilic GNU General Public License v2 or later (GPLv2+) StrongCopyleft\n"
    "Example 1"]
   [["basilic" "GNU General Public License v2 or later (GPLv2+)"
     "StrongCopyleft" ""]
    "%-35s %-55s %-20s"
    "basilic                             GNU General Public License v2 or later (GPLv2+)         StrongCopyleft      \n"
    "Example 2"]])

(deftest test-print-line
  (testing "Print line"
    (doseq [[items fmt expected description] params-print-line]
      (testing description
        (is (= expected (with-out-str (report/print-line items fmt))))))))

(def report
  {:headers {:items ["Dependency" "License Name" "License Type" "Misc"]
             :totals ["License Type" "Found"]}
   :items [{:dependency {:name "aiohttp" :version "3.7.2"}
            :license {:name "Apache Software License" :type "Permissive"}
            :error "Too many requests"}]
   :totals
   {"Permissive" 1}
   :fails nil})

(def params-report
  [[report
    {:verbose false
     :with-totals false
     :table-headers false
     :formatter "%s %s %s"}
    "aiohttp:3.7.2 Apache Software License Permissive\n"
    "Non-verbose, no headers, no-totals"]
   [report
    {:verbose false
     :with-totals false
     :table-headers true
     :formatter "%s %s %s"}
    "Dependency License Name License Type\naiohttp:3.7.2 Apache Software License Permissive\n"
    "Non-verbose, with headers, no-totals"]
   [report
    {:verbose false
     :with-totals true
     :table-headers true
     :formatter "%s %s %s"}
    "Dependency License Name License Type\naiohttp:3.7.2 Apache Software License Permissive\n\nLicense Type Found\nPermissive 1\n"
    "Non-verbose, with headers, with totals"]
   [report
    {:verbose true
     :with-totals true
     :table-headers true
     :formatter "%s %s %s"}
    "Dependency License Name License Type Misc                                    \naiohttp:3.7.2 Apache Software License Permissive Too many requests                       \n\nLicense Type Found\nPermissive 1\n"
    "Verbose, with headers, with totals"]])

(deftest test-print-report
  (testing "Print report"
    (doseq [[report options expected description] params-report]
      (testing description
        (is (= expected (with-out-str (report/print-report report options))))))))
