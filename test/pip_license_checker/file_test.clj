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

(ns pip-license-checker.file-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.file :as file]))

;; file/exists?

(def params-exists?
  [["resources/requirements.txt" true "File exists"]
   ["resources/external.csv" true "File exists"]
   ["no_such_file_here" false "No such file"]])

(deftest test-exists?
  (testing "Check if file exists"
    (doseq [[path expected description] params-exists?]
      (testing description
        (is (= expected (file/exists? path)))))))

;; file/get-requirement-lines

(def mock-path-lines ["a" "b"])

(deftest test-get-requirement-lines
  (testing "Concat of requirement file lines"
    (with-redefs
     [file/path->lines (constantly mock-path-lines)]
      (is (= ["a" "b" "a" "b"]
             (file/get-requirement-lines ["f1.txt" "f2.txt"]))))))

;; file/path->lines

(def params-simple-txt-file ["1" "2" "3"])

(deftest test-path->lines
  (testing "Path to lines vec"
    (is (= params-simple-txt-file
           (file/path->lines "resources/simple.txt")))))

;; file/csv->lines

(def params-simple-csv-file [["package" "license"] ["a" "MIT"] ["b" "BSD"]])

(deftest test-csv->lines
  (testing "Path to lines vec"
    (is (= params-simple-csv-file
           (file/csv->lines "resources/simple.csv")))))

;; file/take-csv-columns

(def params-take-csv-columns
  [[["a" "b" "c" "d"]
    [0 1]
    ["a" "b"]
    "First two columns"]
   [["a" "b" "c" "d"]
    [0 1 3]
    ["a" "b" "d"]
    "Three non-consequtive columns"]
   [["a" "b" "c" "d"]
    [1 10]
    ["b" file/csv-out-of-range-column-index]
    "One valid index, one out of range"]])

(deftest test-take-csv-columns
  (testing "Take columns"
    (doseq [[columns indices expected description] params-take-csv-columns]
      (testing description
        (is (= expected (file/take-csv-columns columns indices)))))))

;; file/csv->data

(def params-csv->data
  [[[["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:skip-header false}
    [{:package "test-package@0.1.2" :license "MIT License"}
     {:package "another-package@21.04" :license "GPLv2"}]
    "No headers"]
   [[["package name" "license name"]
     ["test-package" "BSD License"]
     ["another-package" "EULA"]]
    {:skip-header true}
    [{:package "test-package" :license "BSD License"}
     {:package "another-package" :license "EULA"}]
    "Skip headers"]
   [[["package name" "version" "url" "license name"]
     ["test-package" "0.0.1" "http://example.com/test-package" "BSD License"]
     ["another-package" "21.04" "http://example.com/another-package" "MIT License"]
     ["yet-another-package" "1.0" "http://example.com/yet-another-package" "EPL-2.0 or GPL v2+ Classpath exception"]]
    {:skip-header true :package-column-index 0 :license-column-index 3}
    [{:package "test-package" :license "BSD License"}
     {:package "another-package" :license "MIT License"}
     {:package "yet-another-package" :license "EPL-2.0 or GPL v2+ Classpath exception"}]
    "Specify proper indices"]
   [[["package name" "version" "url" "license name"]
     ["test-package" "0.0.1" "http://example.com/test-package" "BSD License"]
     ["another-package" "21.04" "http://example.com/another-package" "MIT License"]
     ["yet-another-package" "1.0" "http://example.com/yet-another-package" "EPL-2.0 or GPL v2+ Classpath exception"]]
    {:skip-header true :package-column-index 100 :license-column-index 3}
    [{:package nil :license "BSD License"}
     {:package nil :license "MIT License"}
     {:package nil :license "EPL-2.0 or GPL v2+ Classpath exception"}]
    "Package column index is out of range"]
   [[["package name" "version" "url" "license name"]
     ["test-package" "0.0.1" "http://example.com/test-package" "BSD License"]
     ["another-package" "21.04" "http://example.com/another-package" "MIT License"]
     ["yet-another-package" "1.0" "http://example.com/yet-another-package" "EPL-2.0 or GPL v2+ Classpath exception"]]
    {:skip-header true :package-column-index 0 :license-column-index 30}
    [{:package "test-package" :license nil}
     {:package "another-package" :license nil}
     {:package "yet-another-package" :license nil}]
    "License column index is out of range"]])

(deftest test-csv->data
  (testing "Concat of requirement file lines"
    (doseq [[lines options expected description] params-csv->data]
      (testing description
        (with-redefs
         [file/csv->lines (constantly lines)]
          (is (= expected
                 (file/csv->data "fake/path.csv" options))))))))

;; file/edn->data

(def params-edn-item->data-item
  [[[["org.clojars.vrs/gradle-licenses" "0.2.0"] "EPL-2.0"]
    {}
    {:package "org.clojars.vrs/gradle-licenses:0.2.0" :license "EPL-2.0"}
    "Package name and version, no options"]
   [[["org.clojars.vrs/gradle-licenses" "0.2.0"] "EPL-2.0"]
    {:fully-qualified-names false}
    {:package "gradle-licenses:0.2.0" :license "EPL-2.0"}
    "Short names option"]
   [[["org.clojars.vrs/gradle-licenses" ""] "EPL-2.0"]
    {}
    {:package "org.clojars.vrs/gradle-licenses" :license "EPL-2.0"}
    "Version is blank"]
   [[["org.clojars.vrs/gradle-licenses" nil] "EPL-2.0"]
    {}
    {:package "org.clojars.vrs/gradle-licenses" :license "EPL-2.0"}
    "Version is nil"]
   [[[nil "0.2.0"] "EPL-2.0"]
    {}
    {:package nil :license "EPL-2.0"}
    "Package name is nil"]
   [[[nil nil] nil]
    {}
    {:package nil :license nil}
    "All data is nil"]
   [[[nil nil] nil]
    {:fully-qualified-names false}
    {:package nil :license nil}
    "All data is nil, short name option breaks nothing"]])

(deftest test-edn-item->data-item
  (testing "Parse EDN item into package map"
    (doseq [[item options expected description] params-edn-item->data-item]
      (testing description
        (is (= expected (file/edn-item->data-item item options)))))))

(def params-edn->data
  [["([[org.clojars.vrs/gradle-licenses \"0.2.0\"] \"EPL-2.0\"]
      [[commons-io \"2.6\"] \"Apache License, Version 2.0\"]
      [[department/org/another-dep nil] \"MIT License\"])"
    {}
    [{:package "org.clojars.vrs/gradle-licenses:0.2.0" :license "EPL-2.0"}
     {:package "commons-io:2.6" :license "Apache License, Version 2.0"}
     {:package "department/org/another-dep" :license "MIT License"}]
    "Default options"]
   ["([[org.clojars.vrs/gradle-licenses \"0.2.0\"] \"EPL-2.0\"]
      [[commons-io \"2.6\"] \"Apache License, Version 2.0\"]
      [[department/org/another-dep nil] \"MIT License\"])"
    {:fully-qualified-names false}
    [{:package "gradle-licenses:0.2.0" :license "EPL-2.0"}
     {:package "commons-io:2.6" :license "Apache License, Version 2.0"}
     {:package "another-dep" :license "MIT License"}]
    "Turn off fully-qualified names"]])

(deftest test-edn->data
  (testing "EDN file to data"
    (doseq [[content options expected description] params-edn->data]
      (testing description
        (with-redefs
         [file/path->string (constantly content)]
          (is (= expected
                 (file/edn->data "fake/path.csv" options))))))))

(def params-edn->data-integration
  [["resources/external.edn" {} "No options"]
   ["resources/external.edn" {:fully-qualified-names true} "Explicit fully-qualified names"]
   ["resources/external.edn" {:fully-qualified-names false} "Explicit short names"]])

(deftest test-edn->data-integration
  (testing "Parse real EDN file"
    (doseq [[path options description] params-edn->data-integration]
      (testing description
        (is (seq? (file/edn->data path options)))))))

;; Cocoapods

(def cocoapods-default-path "resources/external.cocoapods")

(def cocoapods-default-plist-data
  [{:package "Acknowledgements" :license nil}
   {:package "FBSDKCoreKit" :license "Facebook Platform License"}
   {:package "FirebaseAnalytics" :license "Copyright"}
   {:package "Swinject" :license "MIT"}
   {:package "lottie-ios" :license "Apache"}
   {:package "nanopb" :license "zlib"}
   {:package nil :license nil}])

(def params-cocoapods-plist->data-integration
  [[{} (butlast (rest cocoapods-default-plist-data)) "No options provided, use defaults"]
   [{:skip-header true :skip-footer true} (butlast (rest cocoapods-default-plist-data)) "Skip header explicitly, skip footer explicitly"]
   [{:skip-header false :skip-footer true} (butlast cocoapods-default-plist-data) "Keep header explicitly, skip footer explicitly"]
   [{:skip-header true :skip-footer false} (rest cocoapods-default-plist-data) "Skip header explicitly, keep footer explicitly"]
   [{:skip-footer false} (rest cocoapods-default-plist-data) "Skip header implicitly, keep footer explicitly"]
   [{:skip-header false} (butlast cocoapods-default-plist-data) "Keep header explicitly, skip footer explicitly"]
   [{:skip-header false :skip-footer false} cocoapods-default-plist-data "Keep header explicitly, keep footer explicitly"]])

(deftest test-cocoapods-plist->data-integration
  (testing "Test convertng plist file into clojure map"
    (doseq [[options expected description] params-cocoapods-plist->data-integration]
      (testing description
        (is (= expected (file/cocoapods-plist->data cocoapods-default-path options)))))))
