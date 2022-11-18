;; Copyright © 2020-2022 Vitaly Samigullin
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

(ns pip-license-checker.pypi-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.file :as file]
   [pip-license-checker.github :as github]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
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
         [http/request-get mock]
          (is (= (set expected) (set (pypi/get-releases package-name nil)))))))))

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
         [http/request-get http-get-mock
          pypi/get-releases (constantly releases)]
          (is (= expected (pypi/get-requirement-version requirement {} nil))))))))

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
     "License :: OSI Approved :: GNU General Public License (GPL)"
     "License :: OSI Approved :: GNU Library or Lesser General Public License (LGPL)"
     "License :: OSI Approved :: Mozilla Public License 1.1 (MPL 1.1)"]
    "GNU General Public License (GPL), GNU Library or Lesser General Public License (LGPL), Mozilla Public License 1.1 (MPL 1.1)"
    "Get list of all licenses"]
   [["Operating System :: Unix"
     "License :: OSI Approved :: MIT License"
     "License :: OSI Approved"]
    "MIT License"
    "Get most detailed license"]
   [["Operating System :: Unix"
     "License :: OSI Approved"]
    nil
    "Skip unspecific license classifiers"]])

(deftest test-classifiers->license
  (testing "Get license from trove classifiers"
    (doseq [[classifiers expected description] params-classifiers->license]
      (testing description
        (is (= expected (pypi/classifiers->license classifiers)))))))

;; pypi/data->license-map

(def params-data->license-map
  [[{"info"
     {"license" "MIT"
      "classifiers" ["License :: OSI Approved :: MIT License"]}}
    "MIT"
    {:name "MIT License", :type "Permissive"}
    "Get from classifiers field"]
   [{"info"
     {"license" "MIT"
      "classifiers" ["Operating System :: Unix"]}}
    "BSD"
    {:name "MIT", :type "Permissive"}
    "Get from license field"]
   [{"info"
     {"license" ""
      "classifiers" []}}
    "BSD"
    {:name "BSD", :type "Permissive"}
    "Get from GitHub API"]
   [{"info"
     {"license" "UNKNOWN"
      "classifiers" []}}
    "MIT"
    {:name "MIT", :type "Permissive"}
    "Get from GitHub API for older metadata format for missing license field - UNKNOWN string"]
   [{"info"
     {"license" []
      "classifiers" []}}
    "MIT"
    {:name "MIT", :type "Permissive"}
    "Get from GitHub API for older metadata format for missing license field - empty list"]
   [{"info"
     {"license" ["UNKNOWN"]
      "classifiers" []}}
    "MIT"
    {:name "MIT", :type "Permissive"}
    "Get from GitHub API for older metadata format for missing license field - list with UNKNOWN"]
   [{"wut" 123}
    nil
    license/data-error
    "Error fallback"]])

(deftest test-data->license-map
  (testing "JSON to license map"
    (doseq [[data github-license expected description] params-data->license-map]
      (testing description
        (with-redefs
         [github/homepage->license-name (constantly github-license)]
          (is (= expected (pypi/data->license-map data {} nil))))))))

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
     :license {:name "MIT License", :type "Permissive"}}
    "Ok"]
   [{:ok? false
     :requirement 1
     :data
     {"info"
      {"license" "MIT"
       "classifiers" ["License :: OSI Approved :: MIT License"]}}}
    {:ok? false
     :requirement 1
     :license license/data-error}
    "Fallback"]])

(deftest test-data->license
  (testing "PyPI API data to license"
    (doseq [[data expected description] params-data->license]
      (testing description
        (is (= expected (pypi/data->license data {} nil)))))))


;; get-parsed-requirements


(def params-get-parsed-requirements
  [[[] [] {} "{}" [] "No input"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    "Packages and requirements"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {:exclude #"aio.*" :rate-limits {:requests 1 :millis 60000}}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    "Exclude pattern"]])

(deftest ^:integration ^:request
  test-get-parsed-requirements
  (testing "Integration testing of requirements parsing"
    (doseq [[packages requirements options mock-body expected description]
            params-get-parsed-requirements]
      (testing description
        (with-redefs
          ;; Ugliest hack ever: core_test.clj runs before pypi_test.clj
          ;; and as a part of core/process-requirements testing we shutdown threadpool
          ;; so that when pypi/get-parsed-requiements tries to use the threadpool for pmap
          ;; it's already gone and java.util.concurrent.RejectedExecutionException is thrown.
          ;; Since we are not testing concurrency itself, just monkey-patch pmap with simple map.
         [pmap map
          http/request-get (constantly {:body mock-body})
          pypi/get-releases (constantly [])
          version/get-version (constantly "3.7.2")
          file/get-requirement-lines (fn [_] requirements)]
          (is
           (= expected
              (vec (pypi/get-parsed-requiements
                    packages requirements options)))))))))
