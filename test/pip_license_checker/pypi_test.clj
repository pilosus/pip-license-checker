(ns pip-license-checker.pypi-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.filters :as filters]))


;; pypi/get-requirement-response


(def params-get-requirement-response
  [[{:name "aiohttp" :version "3.7.2"}
    {:ok? true
     :requirement {:name "aiohttp" :version "3.7.2"}
     :response "https://pypi.org/pypi/aiohttp/3.7.2/json"}
    "Specific version"]
   [{:name "django" :version filters/version-latest}
    {:ok? true
     :requirement {:name "django" :version filters/version-latest}
     :response "https://pypi.org/pypi/django/json"}
    "Latest version"]])

(deftest test-get-requirement-response
  (testing "Get requirement response with mock"
    (doseq [[requirement expected description]
            params-get-requirement-response]
      (testing description
        (with-redefs
         [http/get (fn [url & _] {:body url})]
          (is (= expected (pypi/get-requirement-response requirement))))))))

;; pypi/classifiers->license

(def params-classifiers->license
  [[nil nil "No classifiers"]
   [[] nil "Empty classifiers"]
   [["Framework :: Django :: 1.10", "Operating System :: Unix"]
    nil
    "No licenses in classifiers"]
   [["Operating System :: Unix" "License :: OSI Approved :: MIT License"]
    "MIT License"
    "License found"]
   [["Operating System :: Unix"
     "License :: OSI Approved :: BSD License"
     "License :: OSI Approved :: MIT License"]
    "BSD License"
    "Get first license"]])

(deftest test-classifiers->license
  (testing "Get license from trove classifiers"
    (doseq [[classifiers expected description] params-classifiers->license]
      (testing description
        (is (= expected (pypi/classifiers->license classifiers)))))))

;; pypi/strings->pattern

(def params-strings->pattern
  [[[] "(?i)" "Empty pattern"]
   [["^a"] "(?i)(?:^a)" "Pattern 1"]
   [["^a" "b.*"] "(?i)(?:^a)|(?:b.*)" "Pattern 2"]])

(deftest test-strings->pattern
  (testing "Concatenating strings to pattern"
    (doseq [[strings expected description] params-strings->pattern]
      (testing description
        (is (= expected (str (pypi/strings->pattern strings))))))))


;; pypi/license-name->desc


(def params-license-name->desc
  [["MIT License" pypi/license-desc-permissive "Permissive"]
   ["GPLv3" pypi/license-desc-copyleft "Copyleft"]
   ["EULA" pypi/license-desc-other "Other"]])

(deftest test-license-name->desc
  (testing "Get license description by its name"
    (doseq [[license expected description] params-license-name->desc]
      (testing description
        (is (= expected (pypi/license-name->desc license)))))))

;; pypi/data->license-map

(def params-data->license-map
  [[{"info"
     {"license" "MIT"
      "classifiers" ["License :: OSI Approved :: MIT License"]}}
    {:name "MIT License", :desc "Permissive"}
    "Get from classifiers field"]
   [{"info"
     {"license" "MIT"
      "classifiers" ["Operating System :: Unix"]}}
    {:name "MIT", :desc "Permissive"}
    "Get from license field"]
   [{"wut" 123}
    pypi/license-data-error
    "Error fallback"]])

(deftest test-data->license-map
  (testing "JSON to license map"
    (doseq [[data expected description] params-data->license-map]
      (testing description
        (is (= expected (pypi/data->license-map data)))))))
