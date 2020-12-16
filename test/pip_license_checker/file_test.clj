(ns pip-license-checker.file-test
  (:require
   [clojure.test :refer :all]
   [pip-license-checker.file :as file]))

;; file/exists?

(def params-exists?
  [["resources/requirements.txt" true "File exists"]
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
