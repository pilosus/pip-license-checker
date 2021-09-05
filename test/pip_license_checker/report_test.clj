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
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.report :as report]))

(deftest test-print-license-header
  (testing "Printing license table header"
    (let [actual
          (with-out-str
            (report/print-license-header {:formatter report/table-formatter}))
          expected (str/join
                    [(format report/table-formatter
                             "Requirement"
                             "License Name"
                             "License Type")
                     "\n"])]
      (is (= expected actual)))))

(def params-format-license
  [["test"
    "1.12.3"
    "GPLv3"
    "Copyleft"
    (format report/table-formatter "test:1.12.3" "GPLv3" "Copyleft")
    "Example 1"]
   ["aiohttp"
    "3.7.4.post0"
    "MIT"
    "Permissive"
    (format report/table-formatter "aiohttp:3.7.4.post0" "MIT" "Permissive")
    "Example 2"]
   ["aiohttp"
    nil
    "MIT"
    "Permissive"
    (format report/table-formatter "aiohttp" "MIT" "Permissive")
    "Version is absent"]
   ["aiohttp"
    ""
    "MIT"
    "Permissive"
    (format report/table-formatter "aiohttp" "MIT" "Permissive")
    "Version is a blank string"]])

(deftest test-format-license
  (testing "Printing a line of license table"
    (doseq [[package version license-name license-type expected description] params-format-license]
      (testing description
        (let [license-data
              {:requirement {:name package :version version}
               :license {:name license-name :type license-type}}
              actual (report/format-license license-data {})]
          (is (= expected actual)))))))

(deftest test-format-total
  (testing "Formatting total table line"
    (let [expected (format report/totals-formatter "Permissive" 7)
          actual (report/format-total "Permissive" 7 {})]
      (is (= expected actual)))))

(deftest test-print-totals-header
  (testing "Printing totals table header"
    (let [actual (with-out-str (report/print-totals-header {}))
          expected (str/join
                    [(format report/totals-formatter "License Type" "Found" {})
                     "\n"])]
      (is (= expected actual)))))

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
