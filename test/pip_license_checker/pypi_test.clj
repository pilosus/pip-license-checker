;; Copyright © 2020-2023 Vitaly Samigullin
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
   [cheshire.core :as json]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.data :as d]
   [pip-license-checker.file :as file]
   [pip-license-checker.github :as github]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.version :as version]))

(def mock-pypi-api-request (constantly {:status 200 :body "{}"}))

(def mock-pypi-api-throw-exception
  (fn [& _]
    (throw (ex-info
            "Boom!"
            {:status 429 :reason-phrase "Rate limits exceeded"}))))

;; api-simple-get-releases

(def params-api-simple-get-releases
  [["pydantic"
    {"files" [{"filename" "pydantic-1.0.2.tar.gz" "yanked" false} {"filename" "pydantic-1.0.3.tar.gz" "yanked" false}]}
    [["1.0.2" {:yanked false}] ["1.0.3" {:yanked false}]]
    "sdist"]
   ["pydantic"
    {"files"
     [{"filename" "pydantic-1.0.2-py39-none-manylinux_1_1.whl" "yanked" false}
      {"filename" "pydantic-1.0.3-py39-none-manylinux_1_1.whl" "yanked" false}]}
    [["1.0.2" {:yanked false}] ["1.0.3" {:yanked false}]]
    "wheel"]
   ["aiohttp"
    {"files"
     [{"filename" "aiohttp-3.8.1.tar.gz",
       "hashes"
       {"sha256"
        "fc5471e1a54de15ef71c1bc6ebe80d4dc681ea600e68bfd1cbce40427f0b7578"},
       "requires-python" ">=3.6",
       "url"
       "https://files.pythonhosted.org/packages/5a/86/5f63de7a202550269a617a5d57859a2961f3396ecd1739a70b92224766bc/aiohttp-3.8.1.tar.gz",
       "yanked" false}
      {"filename" "aiohttp-3.8.2-cp310-cp310-macosx_10_9_x86_64.whl",
       "hashes"
       {"sha256"
        "66da9965d78206444640fb34364677564b77286463d6aa461a9ae67e09479366"},
       "requires-python" ">=3.6",
       "url"    "https://files.pythonhosted.org/packages/35/61/b15ebc8bc7c274a1b090cf0b638e4140a971bc52ec20bf0cfb00793ee65f/aiohttp-3.8.2-cp310-cp310-macosx_10_9_universal2.whl",
       "yanked" "This version includes overly restrictive multidict upper boundary disallowing multidict v6+. The previous patch version didn't have that and this is now causing dependency resolution problems for the users who have an \"incompatible\" version pinned. This is not really necessary anymore and will be addressed in the next release v3.8.3\r\n\r\nhttps://github.com/aio-libs/aiohttp/pull/6950"}

      {"filename" "aiohttp-3.8.3-cp310-cp310-macosx_10_9_universal2.whl",
       "hashes"
       {"sha256"
        "ba71c9b4dcbb16212f334126cc3d8beb6af377f6703d9dc2d9fb3874fd667ee9"},
       "requires-python" ">=3.6",
       "url"
       "https://files.pythonhosted.org/packages/80/90/e7d60427dfa15b0f3748d6fbb50cc6b0f29112f4f04d8354ac02f65683e1/aiohttp-3.8.3-cp310-cp310-macosx_10_9_universal2.whl",
       "yanked" false}
      {"filename" "aiohttp-3.8.3-cp310-cp310-manylinux_1_1.whl",
       "hashes"
       {"sha256"
        "ea71c9b4dcbb16212f334126cc3d8beb6af377f6703d9dc2d9fb3874fd667ee2"},
       "requires-python" ">=3.6",
       "url"
       "https://files.pythonhosted.org/packages/80/90/e7d60427dfa15b0f3748d6fbb50cc6b0f29112f4f04d8354ac02f65683e1/aiohttp-3.8.3-cp310-cp310-manylinux_1_1.whl",
       "yanked" false}
      {"filename" "aiohttp-4.0.0a1-cp37-cp37m-win_amd64.whl",
       "hashes"
       {"sha256"
        "c94770383e49f9cc5912b926364ad022a6c8a5dbf5498933ca3a5713c6daf738"},
       "requires-python" ">=3.6",
       "url"
       "https://files.pythonhosted.org/packages/8a/fb/7ba4c3fdafa052fe5f2d389261f282ac2190d1a09b25a61621eb6e41c430/aiohttp-4.0.0a1-cp37-cp37m-win_amd64.whl",
       "yanked" false}]}
    [["3.8.1" {:yanked false}] ["3.8.2" {:yanked true}] ["3.8.3" {:yanked false}] ["4.0.0a1" {:yanked false}]]
    "sdist and wheels, exclude yanked versions, ignore duplicates"]])

(deftest ^:request
  test-api-simple-get-releases
  (testing "Get a seq of releases from Simple API"
    (doseq [[package files expected description] params-api-simple-get-releases]
      (let [body (-> files
                     json/generate-string)
            api-mock (constantly {:body body})]
        (testing description
          (with-redefs
           [http/request-get api-mock]
            (let [expected-result (set (map #(apply version/parse-version %) expected))
                  actual-result (set (pypi/api-simple-get-releases package nil))]
              (is (= expected-result actual-result)))))))))

;; pypi/api-get-project

(def params-api-get-project-response
  [[{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    mock-pypi-api-request
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    (d/map->PyPiProject {:status :found
                         :requirement
                         (d/map->Requirement
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
    (d/map->PyPiProject {:status :found
                         :requirement
                         (d/map->Requirement
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
    (d/map->PyPiProject {:status :error
                         :requirement
                         (d/map->Requirement
                          {:name "django"
                           :version "777.666.555"
                           :specifiers [[version/eq (version/parse-version "777.666.555")]]})
                         :api-response nil
                         :license (d/map->License
                                   {:name "Error"
                                    :type "Error"
                                    :error nil})
                         :error "PyPI::version Not found"})
    "Version not found"]
   [{:name "aiohttp"
     :specifiers [[version/eq (version/parse-version "3.7.2")]]}
    mock-pypi-api-throw-exception
    [(version/parse-version "3.7.1") (version/parse-version "3.7.2")]
    (d/map->PyPiProject {:status :error
                         :requirement
                         (d/map->Requirement
                          {:name "aiohttp"
                           :version "3.7.2"
                           :specifiers [[version/eq (version/parse-version "3.7.2")]]})
                         :api-response nil
                         :license (d/map->License
                                   {:name "Error"
                                    :type "Error"
                                    :error nil})
                         :error "PyPI::project 429 Rate limits exceeded"})
    "Expection"]])

(deftest test-api-get-project
  (testing "Get requirement response with mock"
    (doseq [[requirement http-get-mock releases expected description]
            params-api-get-project-response]
      (testing description
        (with-redefs
         [http/request-get http-get-mock
          pypi/api-simple-get-releases (constantly releases)]
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
    (d/->License "MIT" nil nil)
    (d/map->License {:name "MIT License", :type "Permissive" :error nil})
    "Get from classifiers field"]
   [{"info"
     {"license" "MIT"
      "classifiers" ["Operating System :: Unix"]}}
    (d/->License "BSD" nil nil)
    (d/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from license field"]
   [{"info"
     {"license" ""
      "classifiers" []}}
    (d/->License "BSD" nil nil)
    (d/map->License {:name "BSD", :type "Permissive" :error nil})
    "Get from GitHub API"]
   [{"info"
     {"license" "UNKNOWN"
      "classifiers" []}}
    (d/->License "MIT" nil nil)
    (d/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from GitHub API for older metadata format for missing license field - UNKNOWN string"]
   [{"info"
     {"license" []
      "classifiers" []}}
    (d/->License "MIT" nil nil)
    (d/map->License {:name "MIT", :type "Permissive" :error nil})
    "Get from GitHub API for older metadata format for missing license field - empty list"]
   [{"info"
     {"license" ["UNKNOWN"]
      "classifiers" []}}
    (d/->License "MIT" nil nil)
    (d/map->License {:name "MIT", :type "Permissive" :error nil})
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
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
    "Equal =="]
   ["aiohttp===3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/arbitrary-eq (version/parse-version "3.7.2")]]})
    "Equal ==="]
   ["aiohttp!=3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/neq (version/parse-version "3.7.2")]]})
    "Not equal to !="]
   ["aiohttp~=3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/compatible (version/parse-version "3.7.2")]]})
    "Compatible ~="]
   ["aiohttp>3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/gt (version/parse-version "3.7.2")]]})
    "Greater than >"]
   ["aiohttp>=3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/ge (version/parse-version "3.7.2")]]})
    "Greater or equal to >="]
   ["aiohttp<3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/lt (version/parse-version "3.7.2")]]})
    "Less than <"]
   ["aiohttp<=3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers [[version/le (version/parse-version "3.7.2")]]})
    "Less than equal to <="]
   ["aiohttp>=3.7,<4,!=3.7.2"
    (d/map->Requirement
     {:name "aiohttp"
      :specifiers
      [[version/ge (version/parse-version "3.7")]
       [version/lt (version/parse-version "4")]
       [version/neq (version/parse-version "3.7.2")]]})
    "Multiple specifiers"]
   ["aiohttp"
    (d/map->Requirement
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

;; get-parsed-deps

(def params-get-parsed-deps
  [[[] [] {} "{}" [] "No input"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})
       :error nil})
     (d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "test"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})
       :error nil})]
    "Packages and requirements"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {:exclude #"aio.*" :rate-limits {:requests 1 :millis 60000}}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "test"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "MIT License"
                  :type "Permissive"
                  :error nil})
       :error nil})]
    "Exclude pattern"]])

(deftest ^:integration ^:request
  test-get-parsed-deps
  (testing "Integration testing of deps parsing"
    (doseq [[packages requirements options mock-body expected description]
            params-get-parsed-deps]
      (testing description
        (with-redefs
          ;; Ugliest hack ever: core_test.clj runs before pypi_test.clj
          ;; and as a part of core/process-requirements testing we shutdown threadpool
          ;; so that when pypi/get-parsed-deps tries to use the threadpool for pmap
          ;; it's already gone and java.util.concurrent.RejectedExecutionException is thrown.
          ;; Since we are not testing concurrency itself, just monkey-patch pmap with simple map.
         [pmap map
          http/request-get (constantly {:body mock-body})
          version/get-version (constantly "3.7.2")
          file/get-requirement-lines (fn [_] requirements)]
          (is
           (= expected
              (vec (pypi/get-parsed-deps
                    packages requirements options)))))))))

(def params-get-parsed-deps-exceptions
  [[(constantly
     {:body "{\"files\": [{\"filename\": \"aiohttp-3.7.1.tar.gz\", \"yanked\": false}, {\"filename\": \"aiohttp-3.7.2.tar.gz\", \"yanked\": false}]}"})
    (fn [& _] (throw (ex-info "Boom!" {:status 429 :reason-phrase "Rate limits exceeded"})))
    (constantly {:body "{\"license\": {\"name\": \"MIT License\"}}"})
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "Error"
                  :type "Error"
                  :error nil})
       :error "PyPI::project 429 Rate limits exceeded"})]
    "PyPI project request failed"]
   [(fn [& _] (throw (ex-info "Boom!" {:status 404 :reason-phrase "Page not found"})))
    (constantly {:body "{\"info\": {\"license\": \"MIT License\"}}"})
    (constantly {:body "{\"license\": {\"name\": \"MIT License\"}}"})
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "Error"
                  :type "Error"
                  :error nil})
       :error "PyPI::version Not found"})]
    "PyPI version not found"]
   [(constantly
     {:body "{\"files\": [{\"filename\": \"aiohttp-3.7.1.win.zip\", \"yanked\": false}, {\"filename\": \"aiohttp-3.7.2-cp39-cp39-musllinux_1_1_x86_64.whl\", \"yanked\": false}]}"})
    (constantly {:body "{\"info\": {\"home_page\": \"https://github.com/aio-libs/aiohttp\"}}"})
    (fn [& _] (throw (ex-info "Boom!" {:status 429 :reason-phrase "Rate limits exceeded"})))
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "Error"
                  :type "Error"
                  :error "GitHub::license 429 Rate limits exceeded"})
       :error "GitHub::license 429 Rate limits exceeded"})]
    "GitHub link found, but request failed"]
   [(constantly
     {:body "{\"files\": [{\"filename\": \"aiohttp-3.7.1.tar.gz\", \"yanked\": false}, {\"filename\": \"aiohttp-3.7.2.tar.gz\", \"yanked\": false}]}"})
    (constantly {:body "{\"info\": {\"author\": \"me\"}}"})
    (fn [& _] (throw (ex-info "Boom!" {:status 429 :reason-phrase "Rate limits exceeded"})))
    [(d/map->Dependency
      {:requirement (d/map->Requirement
                     {:name "aiohttp"
                      :version "3.7.2"
                      :specifiers [[version/eq (version/parse-version "3.7.2")]]})
       :license (d/map->License
                 {:name "Error"
                  :type "Error"
                  :error github/meta-not-found})
       :error github/meta-not-found})]
    "No data in PyPI, no link to GitHub either"]])

(deftest ^:integration ^:request
  test-get-parsed-deps-exceptions
  (testing "Integration testing of deps parsing with exceptions"
    (doseq [[pypi-releases-mock
             pypi-project-mock
             github-license-mock
             expected
             description]
            params-get-parsed-deps-exceptions]
      (testing description
        (with-redefs
         [pmap map
          pypi/api-simple-request-releases pypi-releases-mock
          pypi/api-request-project pypi-project-mock
          github/api-request-license github-license-mock]
          (is
           (= expected
              (vec (pypi/get-parsed-deps
                    ["aiohttp==3.7.2"] [] {:rate-limits {:requests 1 :millis 60000}})))))))))
