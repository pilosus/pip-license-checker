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
  [[["name"]
    #"(name)"
    "One pattern without modifier"]
   [["name" "[0-9]+"]
    #"(name)|([0-9]+)"
    "Multiple patterns without modifier"]])


(def re-patterns-with-modifier-params
  [[["name"]
    #"(?i)"
    #"(?i)(name)"
    "One pattern with modifier"]
  [["name" "[0-9]+"]
   #"(?i)"
   #"(?i)(name)|([0-9]+)"
   "Multiple patterns with modifier"]])

(deftest test-combine-re-patterns-without-modifier
  (testing "Combine regex patterns"
    (doseq
        [[patterns expected-pattern description]
         re-patterns-no-modifier-params]
      (testing description
        (is
         (= (str expected-pattern)
            (str (core/combine-re-patterns patterns))))))))


(deftest test-combine-re-patterns-with-modifier
  (testing "Combine regex patterns with modifier prefix"
    (doseq
        [[patterns prefix expected-pattern description]
         re-patterns-with-modifier-params]
      (testing description
        (is
         (= (str expected-pattern)
            (str (core/combine-re-patterns prefix patterns))))))))


;; applying re-patterns

(def license-params
  [["GPLv3"
    core/copyleft-license
    "GPL lumped with version"]
   ["gnu general public license"
    core/copyleft-license
    "GPL case insensitive"]
   ["Not exactly GPLv3"
    core/not-copyleft-license
    "GPL at the end"]
   ["Not IBM Public License"
    core/copyleft-license
    "IBM license name in the middle"]
   ["MIT"
    core/not-copyleft-license
    "Permissive license"]
   ["Custom Big Corporation's EULA"
    core/not-copyleft-license
    "Proprietory license"]
   [""
    core/not-copyleft-license
    "Empty license name"]])

(deftest test-get-copyleft-verdict-ok
  (testing "Get copyleft verdict for license name"
    (doseq [[license-name expected-verdict description] license-params]
      (testing description
        (is (= expected-verdict (core/get-copyleft-verdict license-name)))))))


(deftest test-get-copyleft-verdict-ok
  (testing "Nil name throws exception"
    (is (thrown? NullPointerException (core/get-copyleft-verdict nil)))))


;;
;; Find license in API response body
;;

(def response-body-classifiers-one-license
  {:info {:classifiers ["License :: OSI Approved :: Artistic License"]}})

(def response-body-classifiers-many-licenses-and-other-items
  {:info
   {:classifiers
    ["Intended Audience :: Healthcare Industry"
     "License :: OSI Approved :: European Union Public Licence 1.2 (EUPL 1.2)"
     "License :: OSI Approved :: Artistic License"]}})

(def response-body-classifiers-no-license
  {:info {:classifiers ["Intended Audience :: Healthcare Industry"]}})

(def response-body-no-classifiers-no-license
  {:some {:field ["Intended Audience :: Healthcare Industry"]}})

(def response-body-classifiers-and-license
  {:info
   {:license "GPL"
    :classifiers
    ["Intended Audience :: Healthcare Industry"
     "License :: OSI Approved :: GNU General Public License (GPL)"]}})

(def response-body-no-classifiers-with-license {:info {:license "MIT"}})


(def find-license-in-classifiers-params
  [[response-body-classifiers-one-license
    "Artistic License"
    "One license in classifiers"]
   [response-body-classifiers-many-licenses-and-other-items
    "European Union Public Licence 1.2 (EUPL 1.2)"
    "Multiple licenses and other items in classifiers"]
   [response-body-classifiers-and-license
    "GNU General Public License (GPL)"
    "License in classifiers and license"]
   [response-body-classifiers-no-license
    nil
    "No license in classifiers"]
   [response-body-no-classifiers-with-license
    nil
    "No classifiers field"]])


(deftest test-find-license-in-classifiers
  (testing "Find license name in trove classifiers"
    (doseq
        [[body expected-license description]
         find-license-in-classifiers-params]
      (testing description
        (is (= expected-license (core/find-license-in-classifiers body)))))))


(def find-license-params
  [[response-body-classifiers-one-license
    "Artistic License"
    "One license in classifiers"]
   [response-body-classifiers-many-licenses-and-other-items
    "European Union Public Licence 1.2 (EUPL 1.2)"
    "Multiple licenses and other items in classifiers"]
   [response-body-classifiers-and-license
    "GNU General Public License (GPL)"
    "License in classifiers and license"]
   [response-body-classifiers-no-license
    nil
    "No license in classifiers"]
   [response-body-no-classifiers-with-license
    "MIT"
    "No classifiers field"]])


(deftest test-find-license
  (testing "Find license name in either classifiers or license field"
    (doseq
        [[body expected-license description] find-license-params]
      (testing description
        (is (= expected-license (core/find-license body)))))))

;;
;; JSON parsing
;;

(def raw-response-params
  [[{:body "{\"info\": {\"license\": \"BSD\", \"classifiers\": [\"Intended Audience :: Healthcare Industry\"]}}"}
    {:info {:license "BSD" :classifiers ["Intended Audience :: Healthcare Industry"]}}   "Valid JSON with legitimate payload from PyPI"]
   [{:body "{\"error\": {\"message\": \"Something went wrong\", \"code\": 500}}"}
    {:error {:message "Something went wrong" :code 500}}
    "Valid JSON with error payload"]
   [{:body "{\"malformed JSON :-(}"}
    nil
    "Malformed JSON"]])

(deftest test-parse-package-response-body
  (testing "Parse raw response string HTTP GET request"
    (doseq [[body expected-json description]
            raw-response-params]
      (testing description
        (is
         (= expected-json
            (core/parse-package-response-body body)))))))


;;
;; URL generation
;;

(def invalid-path-part "INVALID")

(def get-package-response-params
  [["aiohttp"
   "3.7.2"
   "https://pypi.org/pypi/aiohttp/3.7.2/json"
   "Valid URL for package with explicit version"]
   ["aiohttp"
    core/pypi-latest-version
    "https://pypi.org/pypi/aiohttp/json"
    "Valid URL for package with no excplicit version"]
   [invalid-path-part "1.2.15" nil "Invalid URL"]])


(defn mock-http-get-or-nil
  "Mock function for HTTP GET requests"
  [url]
  (when-not (clojure.string/includes? url invalid-path-part) url))


(deftest test-get-package-response
  (testing "Generate correct URL for PyPI packge details and request it"
    (doseq [[name version expected-url description]
            get-package-response-params]
      (testing description
        (with-redefs
          [core/http-get-or-nil mock-http-get-or-nil]
          (is (= expected-url (core/get-package-response name version))))))))


;;
;; Integration with http request
;;


(def get-license-params
  [[response-body-classifiers-and-license
    "GNU General Public License (GPL)"
    "License found in classifiers field"]
   [response-body-no-classifiers-with-license
    "MIT"
    "License found in license field"]
   [response-body-classifiers-and-license
    "GNU General Public License (GPL)"
    "License from classifiers used instead of license field"]
   [response-body-classifiers-no-license
    core/license-error-name
    "No license in classifiers field"]
   [response-body-no-classifiers-no-license
    core/license-error-name
    "No license, no classifiers fields"]])

(deftest test-get-license
  (testing "Get license name and copyleft verdict"
    (doseq [[parsed-body expected-license description] get-license-params]
      (testing description
       (with-redefs
         [core/parse-package-response-body
          (constantly parsed-body)
          core/http-get-or-nil mock-http-get-or-nil]
         (is (= expected-license (core/get-license "test" "0.1.2"))))))))
