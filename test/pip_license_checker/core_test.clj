;; Tests with CIDER
;; https://docs.cider.mx/cider/testing/running_tests.html
;; Run all project test: C-c C-t p
;; Run all loaded tests: C-c C-t l
;; Run all namespace tests: C-c C-t n
;; Run specific test: C-c C-t t
;; Rerun those failed last time C-c C-t r

(ns pip-license-checker.core-test
  (:require
   [clojure.test :refer :all]
   [pip-license-checker.core :as core]))


(def params-validate-args
  [
   [["--requirements"
     "resources/requirements.txt"
     "django"
     "aiohttp==3.7.1"
     "-r"
     "README.md"]
    {:requirements ["resources/requirements.txt" "README.md"]
     :packages ["django" "aiohttp==3.7.1"]
     :options {}}
    "Normal run"]
   ])

(deftest test-validate-args
  (testing "Validating CLI args"
    (doseq [[args expected description] params-validate-args]
      (testing description
        (is (= expected (core/validate-args args)))))))
