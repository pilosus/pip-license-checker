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
   [clojure.spec.gen.alpha :as gen]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.file :as file]
   [pip-license-checker.github :as github]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.version :as version]))


;; pypi/api-get-releases


(def mock-pypi-api-request (constantly {:status 200 :body "{}"}))

(def mock-pypi-api-throw-exception
  (fn [& _]
    (throw (ex-info
            "Boom!"
            {:status 429 :reason-phrase "Rate limits exceeded"}))))

(def params-api-get-releases
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
    mock-pypi-api-throw-exception
    (map #(version/parse-version %) [])
    "Exception"]])

(deftest ^:request
  test-api-get-releases
  (testing "Get a vec of release versions"
    (doseq [[package-name mock expected description] params-api-get-releases]
      (testing description
        (with-redefs
         [http/request-get mock]
          (is (= (set expected) (set (pypi/api-get-releases package-name nil)))))))))

;; pypi/api-get-project

(def params-api-get-project-response
  [[{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    mock-pypi-api-request
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    (pypi/map->PyPiProject {:status :found
                            :requirement
                            (pypi/map->Requirement
                             {:name "aiohttp"
                              :version "3.7.2"
                              :specifiers [[version/eq (version/parse-version "3.7.2")]]})
                            :api-response {}
                            :license nil
                            :error nil})
    "Specific version"]
   [{:name "django" :specifiers nil}
    mock-pypi-api-request
    [(version/parse-version "3.1.1") (version/parse-version "3.1.2")]
    (pypi/map->PyPiProject {:status :found
                            :requirement
                            (pypi/map->Requirement
                             {:name "django"
                              :version "3.1.2"
                              :specifiers nil})
                            :api-response {}
                            :license nil
                            :error nil})
    "Latest version"]
   [{:name "django"
     :specifiers [[version/eq (version/parse-version "777.666.555")]]}
    mock-pypi-api-request
    [(version/parse-version "3.1.1") (version/parse-version "3.1.2")]
    (pypi/map->PyPiProject {:status :error
                            :requirement
                            (pypi/map->Requirement
                             {:name "django"
                              :version "777.666.555"
                              :specifiers [[version/eq (version/parse-version "777.666.555")]]})
                            :api-response nil
                            :license (license/map->License
                                      {:name "Error"
                                       :type "Error"
                                       :error nil})
                            :error "[PyPI] Version not found"})
    "Version not found"]
   [{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    mock-pypi-api-throw-exception
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    (pypi/map->PyPiProject {:status :error
                            :requirement
                            (pypi/map->Requirement
                             {:name "aiohttp"
                              :version "3.7.2"
                              :specifiers [[version/eq (version/parse-version "3.7.2")]]})
                            :api-response nil
                            :license (license/map->License
                                      {:name "Error"
                                       :type "Error"
                                       :error nil})
                            :error "[PyPI] 429 Rate limits exceeded"})
    "Expection"]])

(deftest test-api-get-project
  (testing "Get requirement response with mock"
    (doseq [[requirement http-get-mock releases expected description]
            params-api-get-project-response]
      (testing description
        (with-redefs
         [http/request-get http-get-mock
          pypi/api-get-releases (constantly releases)]
          (is (= expected (pypi/api-get-project requirement {} nil))))))))


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

(def params-api-response->license-map
  [[{"info"
     {"license" "MIT"
      "classifiers" ["License :: OSI Approved :: MIT License"]}}
    (license/->License "MIT" nil nil)
    (license/map->License {:name "MIT License", :type "Permissive" :error nil})
    "Get from classifiers field"]
   [{"info"
     {"license" "MIT"
      "classifiers" ["Operating System :: Unix"]}}
    (license/->License "BSD" nil nil)
    (license/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from license field"]
   [{"info"
     {"license" ""
      "classifiers" []}}
    (license/->License "BSD" nil nil)
    (license/map->License {:name "BSD", :type "Permissive" :error nil})
    "Get from GitHub API"]
   [{"info"
     {"license" "UNKNOWN"
      "classifiers" []}}
    (license/->License "MIT" nil nil)
    (license/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from GitHub API for older metadata format for missing license field - UNKNOWN string"]
   [{"info"
     {"license" []
      "classifiers" []}}
    (license/->License "MIT" nil nil)
    (license/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from GitHub API for older metadata format for missing license field - empty list"]
   [{"info"
     {"license" ["UNKNOWN"]
      "classifiers" []}}
    (license/->License "MIT" nil nil)
    (license/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from GitHub API for older metadata format for missing license field - list with UNKNOWN"]
   [{"wut" 123}
    nil
    license/license-error
    "Error fallback"]])

(deftest test-api-response->license-map
  (testing "JSON to license map"
    (doseq [[data github-license expected description] params-api-response->license-map]
      (testing description
        (with-redefs
         [github/homepage->license (constantly github-license)]
          (is (= expected (pypi/api-response->license-map data {} nil))))))))

;; pypi/requirement->rec

(def params-requirement->rec
  [["aiohttp==3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
    "Equal =="]
   ["aiohttp===3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/arbitrary-eq (version/parse-version "3.7.2")]]})
    "Equal ==="]
   ["aiohttp!=3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/neq (version/parse-version "3.7.2")]]})
    "Not equal to !="]
   ["aiohttp~=3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/compatible (version/parse-version "3.7.2")]]})
    "Compatible ~="]
   ["aiohttp>3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/gt (version/parse-version "3.7.2")]]})
    "Greater than >"]
   ["aiohttp>=3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/ge (version/parse-version "3.7.2")]]})
    "Greater or equal to >="]
   ["aiohttp<3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/lt (version/parse-version "3.7.2")]]})
    "Less than <"]
   ["aiohttp<=3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/le (version/parse-version "3.7.2")]]})
    "Less than equal to <="]
   ["aiohttp>=3.7,<4,!=3.7.2"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers
      [[version/ge (version/parse-version "3.7")]
       [version/lt (version/parse-version "4")]
       [version/neq (version/parse-version "3.7.2")]]})
    "Multiple specifiers"]
   ["aiohttp"
    (pypi/map->Requirement
     {:name "aiohttp"
      :specifiers nil})
    "No specifiers"]])

(deftest test-requirement->rec
  (testing "Requirement string to a map of name and specifiers"
    (doseq [[requirement expected description] params-requirement->rec]
      (testing description
        (is (= expected (pypi/requirement->rec requirement)))))))

(def params-generators-requirement->rec (gen/sample sp/requirement-str-gen 1000))

(deftest test-generators-requirement->rec
  (testing "Use test.check for generative testing"
    (doseq [requirement params-generators-requirement->rec]
      (testing requirement
        (let [requirement-parsed (pypi/requirement->rec requirement)]
          (is (and
               (map? requirement-parsed)
               (not (nil? (:name requirement-parsed)))
               (not (nil? (:specifiers requirement-parsed))))))))))

;; get-parsed-requirements


(def params-get-parsed-requirements
  [[[] [] {} "{}" [] "No input"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [(pypi/map->PyPiProject
      {:status :found
       :requirement (pypi/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :api-response {"info" {"license" "MIT License"}}
       :license (license/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})})
     (pypi/map->PyPiProject
      {:status :found
       :requirement (pypi/map->Requirement
                     {:name "test"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :api-response {"info" {"license" "MIT License"}}
       :license (license/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})})]
    "Packages and requirements"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {:exclude #"aio.*" :rate-limits {:requests 1 :millis 60000}}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [(pypi/map->PyPiProject
      {:status :found
       :requirement (pypi/map->Requirement
                     {:name "test"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :api-response {"info" {"license" "MIT License"}}
       :license (license/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})})]
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
          pypi/api-get-releases (constantly [])
          version/get-version (constantly "3.7.2")
          file/get-requirement-lines (fn [_] requirements)]
          (is
           (= expected
              (vec (pypi/get-parsed-requiements
                    packages requirements options)))))))))
