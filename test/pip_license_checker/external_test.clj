;; Copyright © 2020, 2021 Vitaly Samigullin
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
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.file :as file]
   [pip-license-checker.external :as external]))

(def params-package-name->requirement-map
  [[nil {:name nil :version nil} "No package"]
   ["test-package" {:name "test-package" :version nil} "No separators"]
   ["test-package_0.10.0" {:name "test-package_0.10.0" :version nil} "Unknown separator"]
   ["node-forge@0.10.0" {:name "node-forge" :version "0.10.0"} "Single @ separator"]
   ["node-forge:0.10.0" {:name "node-forge" :version "0.10.0"} "Single : separator"]
   ["@org-name/node-forge@0.10.0" {:name "org-name/node-forge" :version "0.10.0"} "Multiple @ separators"]])

(deftest test-remove-requirements-internal-rules
  (testing "Test formatting package string into requirement map"
    (doseq [[package expected description] params-package-name->requirement-map]
      (testing description
        (is (= expected (external/package-name->requirement-map package)))))))

(def params-license-name->map
  [["MIT License" {:name "MIT License" :type "Permissive"} "Permissive license"]
   ["GPL v3 or any later" {:name "GPL v3 or any later" :type "StrongCopyleft"} "Copyleft license"]
   ["Imaginary License" {:name "Imaginary License" :type "Other"} "Unknown license"]])

(deftest test-license-name->map
  (testing "Test license name formatting"
    (doseq [[license expected description] params-license-name->map]
      (testing description
        (is (= expected (external/license-name->map license)))))))

(def params-external-obj->requirement
  [[{:package "test-package@0.1.2" :license "MIT License"}
    {:ok? true
     :requirement {:name "test-package" :version "0.1.2"}
     :license {:name "MIT License" :type "Permissive"}}
    "Test 1"]
   [{:package "test-package" :license "LGPL"}
    {:ok? true
     :requirement {:name "test-package" :version nil}
     :license {:name "LGPL" :type "WeakCopyleft"}}
    "Test 2"]])

(deftest test-external-obj->requirement
  (testing "Test converting external object to requirement"
    (doseq [[external-obj expected description] params-external-obj->requirement]
      (testing description
        (is (= expected (external/external-obj->requirement external-obj)))))))

(def params-get-parsed-requiements-csv
  [[[["package name" "license name"]
     ["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:external-options {:skip-header true}}
    [{:ok? true
      :requirement {:name "test-package" :version "0.1.2"}
      :license {:name "MIT License" :type "Permissive"}}
     {:ok? true
      :requirement {:name "another-package" :version "21.04"}
      :license {:name "GPLv2" :type "StrongCopyleft"}}]
    "No headers"]
   [[["package name" "license name"]
     ["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:external-options {:skip-header true} :exclude #"another-.*"}
    [{:ok? true
      :requirement {:name "test-package" :version "0.1.2"}
      :license {:name "MIT License" :type "Permissive"}}]
    "Exclude pattern"]])

(deftest test-get-parsed-requiements-csv
  (testing "Test license name formatting"
    (doseq [[external-lines options expected description] params-get-parsed-requiements-csv]
      (testing description
        (with-redefs
         [file/csv->lines (constantly external-lines)]
          (is (= expected (external/get-parsed-requiements ["placeholder"] options))))))))

(def params-get-parsed-requiements-cocoapods
  [[[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-cocoapods}
    [{:ok? true
      :requirement {:name "test-package" :version nil}
      :license {:name "MIT License" :type "Permissive"}}
     {:ok? true
      :requirement {:name "another-package" :version nil}
      :license {:name "GPLv2" :type "StrongCopyleft"}}]
    "No headers"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-cocoapods :exclude #"another-.*"}
    [{:ok? true
      :requirement {:name "test-package" :version nil}
      :license {:name "MIT License" :type "Permissive"}}]
    "Exclude pattern"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-gradle :exclude #"another-.*"}
    [{:ok? true
      :requirement {:name "test-package" :version nil}
      :license {:name "MIT License" :type "Permissive"}}]
    "Gradle license plugin"]])

(deftest test-get-parsed-requiements-external-plugins
  (testing "Test license name formatting"
    (doseq [[external-data options expected description] params-get-parsed-requiements-cocoapods]
      (testing description
        #_:clj-kondo/ignore
        (with-redefs
         [cocoapods-acknowledgements-licenses.core/plist->data (constantly external-data)
          gradle-licenses.core/gradle-json->data (constantly external-data)]
          (is (= expected (external/get-parsed-requiements ["placeholder"] options))))))))
