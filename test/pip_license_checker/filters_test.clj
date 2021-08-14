(ns pip-license-checker.filters-test
  (:require
   [clojure.spec.gen.alpha :as gen]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.spec :as sp]
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

(def params-generators-requirements (gen/sample sp/requirements-str-gen 100))
(def params-generators-pattern (map re-pattern (gen/sample sp/non-empty-str-gen 100)))

(deftest test-generators-remove-requirements-internal-rules
  (testing "Generative test using test.check for internal requirements filtering"
    (doseq [requirements params-generators-requirements]
      (testing requirements
        (let [result (filters/remove-requirements-internal-rules requirements)]
          (is (seq? result)))))))


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

(deftest test-generators-remove-requirements-user-rules
  (testing "Generative test using test.check for user requirements filtering"
    (doseq [[pattern requirements]
            (map
             list
             params-generators-pattern
             params-generators-requirements)]
      (testing (str pattern requirements)
        (let [result
              (filters/remove-requirements-user-rules pattern requirements)]
          (is (seq? result)))))))


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
  (testing "Requirement string to a map of name and specifiers"
    (doseq [[requirement expected description] params-requirement->map]
      (testing description
        (is (= expected (filters/requirement->map requirement)))))))

(def params-generators-requirement->map (gen/sample sp/requirement-str-gen 1000))

(deftest test-generators-requirement->map
  (testing "Use test.check for generative testing"
    (doseq [requirement params-generators-requirement->map]
      (testing requirement
        (let [requirement-parsed (filters/requirement->map requirement)]
          (is (and
               (map? requirement-parsed)
               (not (nil? (:name requirement-parsed)))
               (not (nil? (:specifiers requirement-parsed))))))))))

(def params-filter-parsed-requirements
  [[[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{"Copyleft"} :fails-only true}
    [{:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "Filter copyleft licenses"]
   [[]
    {:fail #{"Copyleft"} :fails-only true}
    []
    "Filter empty sequence"]
   [[]
    {:fail #{"Copyleft"} :fails-only true}
    []
    "Filter nil sequence"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{"Copyleft"} :fails-only false}
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "fails-only flag is off, do not filter"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{} :fails-only true}
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "fail flag omitted, do not filter"]])

(deftest ^:integration ^:request
  test-filter-parsed-requirements
  (testing "Integration testing for filtering parsed requirements"
    (doseq [[licenses options expected description]
            params-filter-parsed-requirements]
      (testing description
        (is (= expected (filters/filter-parsed-requirements licenses options)))))))
