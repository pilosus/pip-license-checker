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

(def params-print-license-header
  [[{:formatter report/table-formatter}
    (format report/table-formatter
            "Requirement"
            "License Name"
            "License Type")
    "Normal output"]
   [{:formatter report/table-formatter :verbose true}
    (format (format "%s %s" report/table-formatter report/verbose-formatter)
            "Requirement"
            "License Name"
            "License Type"
            "Misc")
    "Verbose output"]])

(deftest test-print-license-header
  (testing "Printing license table header"
    (doseq [[options expected description] params-print-license-header]
      (testing description
        (let [actual (with-out-str (report/print-license-header options))
              result (str/join [expected "\n"])]
          (is (= result actual)))))))

(def params-format-license
  [[{:requirement {:name "test" :version "1.12.3"}
     :license {:name "GPLv3" :type "Copyleft"}}
    {}
    (format report/table-formatter "test:1.12.3" "GPLv3" "Copyleft")
    "Example 1"]
   [{:requirement {:name "aiohttp" :version "3.7.4.post0"}
     :license {:name "MIT" :type "Permissive"}}
    {}
    (format report/table-formatter "aiohttp:3.7.4.post0" "MIT" "Permissive")
    "Example 2"]
   [{:requirement {:name "aiohttp" :version nil}
     :license {:name "MIT" :type "Permissive"}}
    {}
    (format report/table-formatter "aiohttp" "MIT" "Permissive")
    "Version is absent"]
   [{:requirement {:name "aiohttp" :version ""}
     :license {:name "MIT" :type "Permissive"}}
    {}
    (format report/table-formatter "aiohttp" "MIT" "Permissive")
    "Version is a blank string"]
   [{:requirement {:name "aiohttp" :version "777.1.2"}
     :license {:name "Error" :type "Error" :error nil}
     :error "[PyPI] Version not found"}
    {:verbose true}
    (format
     (format "%s %s" report/table-formatter report/verbose-formatter)
     "aiohttp:777.1.2" "Error" "Error" "[PyPI] Version not found")
    "Verbose output"]])

(deftest test-format-license
  (testing "Printing a line of license table"
    (doseq [[license-data opts expected description] params-format-license]
      (testing description
        (is (= expected (report/format-license license-data opts)))))))

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
