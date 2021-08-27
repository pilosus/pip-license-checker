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
    "Skip headers"]])

(deftest test-csv->data
  (testing "Concat of requirement file lines"
    (doseq [[lines options expected description] params-csv->data]
      (testing description
        (with-redefs
         [file/csv->lines (constantly lines)]
          (is (= expected
                 (file/csv->data "fake/path.csv" options))))))))
