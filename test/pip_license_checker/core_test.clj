;; Tests with CIDER
;; https://docs.cider.mx/cider/testing/running_tests.html
;; Run all project test: C-c C-t p
;; Run all loaded tests: C-c C-t l
;; Run all namespace tests: C-c C-t n
;; Run specific test: C-c C-t t
;; Rerun those failed last time C-c C-t r

(ns pip-license-checker.core-test
  (:require [clojure.test :refer :all]
            [pip-license-checker.core :as core]))

;;
;; Copyleft verdict
;;

;; re-patterns concatenation

(def re-patterns-no-modifier-params
  [[["name"] #"(name)" "One pattern without modifier"]
   [["name" "[0-9]+"] #"(name)|([0-9]+)" "Multiple patterns without modifier"]])


(def re-patterns-with-modifier-params
  [[["name"] #"(?i)" #"(?i)(name)" "One pattern with modifier"]
  [["name" "[0-9]+"] #"(?i)" #"(?i)(name)|([0-9]+)" "Multiple patterns with modifier"]])

(deftest test-combine-re-patterns-without-modifier
  (testing "Combine regex patterns"
    (doseq [[patterns expected-pattern description] re-patterns-no-modifier-params]
      (testing description
        (is (= (str expected-pattern) (str (core/combine-re-patterns patterns))))))))


(deftest test-combine-re-patterns-with-modifier
  (testing "Combine regex patterns with modifier prefix"
    (doseq
        [[patterns prefix expected-pattern description] re-patterns-with-modifier-params]
      (testing description
        (is
         (= (str expected-pattern)
            (str (core/combine-re-patterns prefix patterns))))))))


;; applying re-patterns

(def license-params
  [["GPLv3" core/copyleft-license "GPL lumped with version"]
   ["gnu general public license" core/copyleft-license "GPL case insensitive"]
   ["Not exactly GPLv3" core/not-copyleft-license "GPL at the end"]
   ["Not IBM Public License" core/copyleft-license "IBM license name in the middle"]
   ["MIT" core/not-copyleft-license "Permissive license"]
   ["Custom Big Corporation's EULA" core/not-copyleft-license "Proprietory license"]
   ["" core/not-copyleft-license "Empty license name"]])

(deftest test-get-copyleft-verdict-ok
  (testing "Get copyleft verdict for license name"
    (doseq [[license-name expected-verdict description] license-params]
      (testing description
        (is (= expected-verdict (core/get-copyleft-verdict license-name)))))))


(deftest test-get-copyleft-verdict-ok
  (testing "Nil name throws exception"
    (is (thrown? NullPointerException (core/get-copyleft-verdict nil)))))
