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

(ns pip-license-checker.external-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.data :as d]
   [pip-license-checker.external :as external]
   [pip-license-checker.file :as file]))

(def params-package-name->requirement
  [[nil
    (d/map->Requirement {:name nil :version nil})
    "No package"]
   ["test-package"
    (d/map->Requirement {:name "test-package" :version nil})
    "No separators"]
   ["test-package_0.10.0"
    (d/map->Requirement {:name "test-package_0.10.0" :version nil})
    "Unknown separator"]
   ["node-forge@0.10.0"
    (d/map->Requirement {:name "node-forge" :version "0.10.0"})
    "Single @ separator"]
   ["node-forge:0.10.0"
    (d/map->Requirement {:name "node-forge" :version "0.10.0"})
    "Single : separator"]
   ["@org-name/node-forge@0.10.0"
    (d/map->Requirement {:name "org-name/node-forge" :version "0.10.0"})
    "Multiple @ separators"]])

(deftest test-remove-requirements-internal-rules
  (testing "Test formatting package string into requirement map"
    (doseq [[package expected description] params-package-name->requirement]
      (testing description
        (is (= expected (external/package-name->requirement package)))))))

(def params-license-name->map
  [["MIT License"
    (d/map->License
     {:name "MIT License" :type "Permissive"})
    "Permissive license"]
   ["GPL v3 or any later"
    (d/map->License
     {:name "GPL v3 or any later" :type "StrongCopyleft" :error nil})
    "Copyleft license"]
   ["Imaginary License"
    (d/map->License
     {:name "Imaginary License" :type "Other" :error nil})
    "Unknown license"]])

(deftest test-license-name->map
  (testing "Test license name formatting"
    (doseq [[license expected description] params-license-name->map]
      (testing description
        (is (= expected (external/license-name->map license)))))))

(def params-external-obj->dep
  [[{:package "test-package@0.1.2" :license "MIT License"}
    (d/map->Dependency
     {:requirement (d/map->Requirement {:name "test-package" :version "0.1.2"})
      :license (d/map->License
                {:name "MIT License" :type "Permissive" :error nil})})
    "Test 1"]
   [{:package "test-package" :license "LGPL"}
    (d/map->Dependency
     {:requirement (d/map->Requirement {:name "test-package" :version nil})
      :license (d/map->License
                {:name "LGPL" :type "WeakCopyleft" :error nil})})
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
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version "0.1.2"})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})
     (d/map->Dependency
      {:requirement (d/map->Requirement {:name "another-package" :version "21.04"})
       :license (d/map->License
                 {:name "GPLv2" :type "StrongCopyleft" :error nil})})]
    "No headers"]
   [[["package name" "license name"]
     ["test-package@0.1.2" "MIT License"]
     ["another-package@21.04" "GPLv2"]]
    {:external-options {:skip-header true} :exclude #"another-.*"}
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version "0.1.2"})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})]
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
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version nil})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})
     (d/map->Dependency
      {:requirement (d/map->Requirement {:name "another-package" :version nil})
       :license (d/map->License
                 {:name "GPLv2" :type "StrongCopyleft" :error nil})})]
    "No headers"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-cocoapods :exclude #"another-.*"}
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version nil})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})]
    "Exclude pattern"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-gradle :exclude #"another-.*"}
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version nil})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})]
    "Gradle license plugin"]
   [[{:package "test-package" :license "MIT License"}
     {:package "another-package" :license "GPLv2"}]
    {:external-format external/format-edn :exclude #"another-.*"}
    [(d/map->Dependency
      {:requirement (d/map->Requirement {:name "test-package" :version nil})
       :license (d/map->License
                 {:name "MIT License" :type "Permissive" :error nil})})]
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
