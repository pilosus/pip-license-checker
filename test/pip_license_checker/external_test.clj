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

(ns pip-license-checker.external-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.external :as external]
   [pip-license-checker.file :as file]
   [pip-license-checker.spec :as sp]
))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

(def params-package-name->requirement
  [[nil
    (s/assert ::sp/requirement {:name nil :version nil})
    "No package"]
   ["test-package"
    (s/assert ::sp/requirement {:name "test-package" :version nil})
    "No separators"]
   ["test-package_0.10.0"
    (s/assert ::sp/requirement {:name "test-package_0.10.0" :version nil})
    "Unknown separator"]
   ["node-forge@0.10.0"
    (s/assert ::sp/requirement {:name "node-forge" :version "0.10.0"})
    "Single @ separator"]
   ["node-forge:0.10.0"
    (s/assert ::sp/requirement {:name "node-forge" :version "0.10.0"})
    "Single : separator"]
   ["@org-name/node-forge@0.10.0"
    (s/assert ::sp/requirement {:name "org-name/node-forge" :version "0.10.0"})
    "Multiple @ separators"]])

(deftest test-remove-requirements-internal-rules
  (testing "Test formatting package string into requirement map"
    (doseq [[package expected description] params-package-name->requirement]
      (testing description
        (is (= expected (external/package-name->requirement package)))))))

(def params-license-name->map
  [["MIT License"
    (s/assert
     ::sp/license
     {:name "MIT License" :type "Permissive" :logs nil})
    "Permissive license"]
   ["GPL v3 or any later"
    (s/assert
     ::sp/license
     {:name "GPL v3 or any later" :type "StrongCopyleft" :logs nil})
    "Copyleft license"]
   ["Imaginary License"
    (s/assert
     ::sp/license
     {:name "Imaginary License" :type "Other" :logs nil})
    "Unknown license"]])

(deftest test-license-name->map
  (testing "Test license name formatting"
    (doseq [[license expected description] params-license-name->map]
      (testing description
        (is (= expected (external/license-name->map license)))))))

(def params-external-obj->dep
  [[{:package "test-package@0.1.2" :license "MIT License"}
    (s/assert
     ::sp/dependency
     {:requirement
      (s/assert
       ::sp/requirement
       {:name "test-package" :version "0.1.2"})
      :license
      (s/assert
       ::sp/license
       {:name "MIT License" :type "Permissive" :logs nil})
      :logs nil})
    "Test 1"]
   [{:package "test-package" :license "LGPL"}
    (s/assert
     ::sp/dependency
     {:requirement
      (s/assert
       ::sp/requirement {:name "test-package" :version nil})
      :license
      (s/assert
       ::sp/license
       {:name "LGPL" :type "WeakCopyleft" :logs nil})
      :logs nil})
    "Test 2"]])

(deftest test-external-obj->dep
  (testing "Test converting external object to requirement"
    (doseq [[external-obj expected description] params-external-obj->dep]
      (testing description
        (is (= expected (external/external-obj->dep external-obj)))))))

(def params-get-parsed-deps-csv
  [[[["package name" "license name"]
     ["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:external-options {:skip-header true}}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "test-package" :version "0.1.2"})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})
     (s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement {:name "another-package" :version "21.04"})
       :license
       (s/assert
        ::sp/license
        {:name "GPLv2" :type "StrongCopyleft" :logs nil})
       :logs nil})]
    "No headers"]
   [[["package name" "license name"]
     ["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:external-options {:skip-header true} :exclude #"another-.*"}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "test-package" :version "0.1.2"})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})]
    "Exclude pattern"]])

(deftest test-get-parsed-requiements-csv
  (testing "Test license name formatting"
    (doseq [[external-lines options expected description] params-get-parsed-deps-csv]
      (testing description
        (with-redefs
         [file/csv->lines (constantly external-lines)]
          (is (= expected (external/get-parsed-deps ["placeholder"] options))))))))

(def params-get-parsed-deps-cocoapods
  [[[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-cocoapods}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "test-package" :version nil})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})
     (s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "another-package" :version nil})
       :license
       (s/assert
        ::sp/license
        {:name "GPLv2" :type "StrongCopyleft" :logs nil})
       :logs nil})]
    "No headers"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-cocoapods :exclude #"another-.*"}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "test-package" :version nil})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})]
    "Exclude pattern"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-gradle :exclude #"another-.*"}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement
        {:name "test-package" :version nil})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})]
    "Gradle license plugin"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-edn :exclude #"another-.*"}
    [(s/assert
      ::sp/dependency
      {:requirement
       (s/assert
        ::sp/requirement {:name "test-package" :version nil})
       :license
       (s/assert
        ::sp/license
        {:name "MIT License" :type "Permissive" :logs nil})
       :logs nil})]
    "EDN plugin"]])

(deftest test-get-parsed-deps-external-plugins
  (testing "Test license name formatting"
    (doseq [[external-data options expected description] params-get-parsed-deps-cocoapods]
      (testing description
        #_:clj-kondo/ignore
        (with-redefs
         [file/cocoapods-plist->data (constantly external-data)
          file/gradle-json->data (constantly external-data)
          file/edn->data (constantly external-data)]
          (is (= expected (external/get-parsed-deps ["placeholder"] options))))))))
