(ns pip-license-checker.filters-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.version :as version]))

;; filters/remove-requirements-internal-rules

(def params-remove-requirements-internal-rules
  [[[] [] "No requirements"]
   [nil [] "Requirements is nil"]
   [["  # this is a comment" "django"] ["django"] "Comments"]
   [["-r resources/requirements.txt" "django"] ["django"] "Option 1"]
   [[" --requirement resources/requirements.txt" "django"] ["django"] "Option 2"]
   [["" "django"] ["django"] "Blank line 1"]
   [[" \n" "django"] ["django"] "Blank line 2"]
   [["http://example.com/package.whl" "django"] ["django"] "Url 1"]
   [[" https://example.com/package.whl" "django"] ["django"] "Url 2"]])

(deftest test-remove-requirements-internal-rules
  (testing "Removing lines from requirements with internal rules"
    (doseq [[requirements expected description]
            params-remove-requirements-internal-rules]
      (testing description)
      (is
       (= expected
          (filters/remove-requirements-internal-rules requirements))))))

;; filters/remove-requirements-user-rules

(def params-remove-requirements-user-rules
  [[[] #"test" [] "No requirements"]
   [nil #"test" [] "Requirements is nil"]
   [["aiohttp==3.7.2" "django"] #"aio.*" ["django"] "Pattern 1"]
   [["aiohttp==3.7.2" "django"] #".*" [] "Pattern 2"]])

(deftest test-remove-requirements-user-rules
  (testing "Removing lines from requirements with user-defined rules"
    (doseq [[requirements pattern expected description]
            params-remove-requirements-user-rules]
      (testing description)
      (is
       (= expected
          (filters/remove-requirements-user-rules pattern requirements))))))

;; filters/sanitize-requirement

(def params-sanitize-requirement
  [["  hello == 1.2.3" "hello==1.2.3" "Whitespaces"]
   ["  hello == 1.2.3  #comment" "hello==1.2.3" "Comment"]
   ["  hello == 1.2.3 ; python_version > '3.6'" "hello==1.2.3" "Modifiers"]])

(deftest test-sanitize-requirement
  (testing "Sanitizing requirement string"
    (doseq [[requirement expected description] params-sanitize-requirement]
      (testing description
        (is (= expected (filters/sanitize-requirement requirement)))))))


;; filters/requirement->map


(def params-requirement->map
  [["aiohttp==3.7.2"
    {:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    "Equal =="]
   ["aiohttp===3.7.2"
    {:name "aiohttp"
     :specifiers [[version/arbitrary-eq (version/parse-version "3.7.2")]]}
    "Equal ==="]
   ["aiohttp!=3.7.2"
    {:name "aiohttp"
     :specifiers [[version/neq (version/parse-version "3.7.2")]]}
    "Not equal to !="]
   ["aiohttp~=3.7.2"
    {:name "aiohttp"
     :specifiers [[version/compatible (version/parse-version "3.7.2")]]}
    "Compatible ~="]
   ["aiohttp>3.7.2"
    {:name "aiohttp"
     :specifiers [[version/gt (version/parse-version "3.7.2")]]}
    "Greater than >"]
   ["aiohttp>=3.7.2"
    {:name "aiohttp"
     :specifiers [[version/ge (version/parse-version "3.7.2")]]}
    "Greater or equal to >="]
   ["aiohttp<3.7.2"
    {:name "aiohttp"
     :specifiers [[version/lt (version/parse-version "3.7.2")]]}
    "Less than <"]
   ["aiohttp<=3.7.2"
    {:name "aiohttp"
     :specifiers [[version/le (version/parse-version "3.7.2")]]}
    "Less than equal to <="]
   ["aiohttp>=3.7,<4,!=3.7.2"
    {:name "aiohttp"
     :specifiers
     [[version/ge (version/parse-version "3.7")]
      [version/lt (version/parse-version "4")]
      [version/neq (version/parse-version "3.7.2")]]}
    "Multiple specifiers"]
   ["aiohttp"
    {:name "aiohttp"
     :specifiers nil}
    "No specifiers"]])

(deftest test-requirement->map
  (testing ""
    (doseq [[requirement expected description] params-requirement->map]
      (testing description
        (is (= expected (filters/requirement->map requirement)))))))
