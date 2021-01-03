(ns pip-license-checker.pypi-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [pip-license-checker.github :as github]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.version :as version]))


;; pypi/get-releases


(def http-get-throw-exeption (fn [& _] (throw (Exception. "Boom!"))))

(def params-get-releases
  [["aiohttp"
    (constantly
     {:body "{\"releases\": {\"1.0.0\": [], \"2.1.3\": []}}"})
    (map #(version/parse-version %) ["1.0.0" "2.1.3"])
    "Ok"]
   ["ipython"
    (constantly
     {:body "{\"releases\": {\"1.0.0\": [], \"0.7.4.svn.r2010\": []}}"})
    (map #(version/parse-version %) ["1.0.0"])
    "Skip invalid versions"]
   ["no-such-package"
    (constantly {:body "{\"releases\": {}}"})
    (map #(version/parse-version %) [])
    "No versions"]
   ["aiohttp"
    ;; cannot use constantly as it gets evaluated where defined
    ;; use lambda expression instead
    http-get-throw-exeption
    (map #(version/parse-version %) [])
    "Exception"]])

(deftest ^:request
  test-get-releases
  (testing "Get a vec of release versions"
    (doseq [[package-name mock expected description] params-get-releases]
      (testing description
        (with-redefs
         [http/get mock]
          (is (= (set expected) (set (pypi/get-releases package-name)))))))))

;; pypi/get-requirement-version

(def http-get-body-with-url (fn [url & _] {:body url}))

(def params-get-requirement-response
  [[{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    http-get-body-with-url
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    {:ok? true
     :requirement {:name "aiohttp" :version "3.7.2"}
     :response "https://pypi.org/pypi/aiohttp/3.7.2/json"}
    "Specific version"]
   [{:name "django" :specifiers nil}
    http-get-body-with-url
    [(version/parse-version "3.1.1") (version/parse-version "3.1.2")]
    {:ok? true
     :requirement {:name "django" :version "3.1.2"}
     :response "https://pypi.org/pypi/django/3.1.2/json"}
    "Latest version"]
   [{:name "django"
     :specifiers [[version/eq (version/parse-version "777.666.555")]]}
    http-get-body-with-url
    [(version/parse-version "3.1.1") (version/parse-version "3.1.2")]
    {:ok? false
     :requirement {:name "django" :version "777.666.555"}}
    "No such version"]
   [{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    http-get-throw-exeption
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    {:ok? false
     :requirement {:name "aiohttp" :version "3.7.2"}}
    "Expection"]])

(deftest test-get-requirement-version
  (testing "Get requirement response with mock"
    (doseq [[requirement http-get-mock releases expected description]
            params-get-requirement-response]
      (testing description
        (with-redefs
         [http/get http-get-mock
          pypi/get-releases (constantly releases)]
          (is (= expected (pypi/get-requirement-version requirement))))))))

;; pypi/requirement-response->data

(def params-requirement-response->data
  [[{:ok? true :requirement 1 :response "{}"}
    {:ok? true :requirement 1 :data {}}
    "Ok"]
   [{:ok? false :requirement 1 :response "{}"}
    {:ok? false :requirement 1}
    "Not ok"]])

(deftest test-requirement-response->data
  (testing "Requirement to data"
    (doseq [[response expected description] params-requirement-response->data]
      (testing description
        (is (= expected (pypi/requirement-response->data response)))))))

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
    "MIT"
    {:name "MIT License", :desc "Permissive"}
    "Get from classifiers field"]
   [{"info"
     {"license" "MIT"
      "classifiers" ["Operating System :: Unix"]}}
    "BSD"
    {:name "MIT", :desc "Permissive"}
    "Get from license field"]
   [{"info"
     {"license" ""
      "classifiers" []}}
    "BSD"
    {:name "BSD", :desc "Permissive"}
    "Get from GitHub API"]
   [{"wut" 123}
    nil
    pypi/license-data-error
    "Error fallback"]])

(deftest test-data->license-map
  (testing "JSON to license map"
    (doseq [[data github-license expected description] params-data->license-map]
      (testing description
        (with-redefs
         [github/homepage->license-name (constantly github-license)]
          (is (= expected (pypi/data->license-map data))))))))

;; pypi/data->license

(def params-data->license
  [[{:ok? true
     :requirement 1
     :data
     {"info"
      {"license" "MIT"
       "classifiers" ["License :: OSI Approved :: MIT License"]}}}
    {:ok? true
     :requirement 1
     :license {:name "MIT License", :desc "Permissive"}}
    "Ok"]
   [{:ok? false
     :requirement 1
     :data
     {"info"
      {"license" "MIT"
       "classifiers" ["License :: OSI Approved :: MIT License"]}}}
    {:ok? false
     :requirement 1
     :license pypi/license-data-error}
    "Fallback"]])

(deftest test-data->license
  (testing "PyPI API data to license"
    (doseq [[data expected description] params-data->license]
      (testing description
        (is (= expected (pypi/data->license data)))))))
