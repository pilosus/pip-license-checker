(ns pip-license-checker.license-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.license :as license]))


;; license/strings->pattern


(def params-strings->pattern
  [[[] "(?i)" "Empty pattern"]
   [["^a"] "(?i)(?:^a)" "Pattern 1"]
   [["^a" "b.*"] "(?i)(?:^a)|(?:b.*)" "Pattern 2"]])

(deftest test-strings->pattern
  (testing "Concatenating strings to pattern"
    (doseq [[strings expected description] params-strings->pattern]
      (testing description
        (is (= expected (str (license/strings->pattern strings))))))))


;; license/name->type


(def params-name->type
  [["MIT License" license/type-permissive "Permissive"]
   ["Artistic license" license/type-permissive "Permissive"]
   ["zope public license" license/type-permissive "Permissive"]
   ["WTFPL" license/type-permissive "Permissive"]
   ["CC0" license/type-permissive "Permissive"]
   ["Mozilla Public License 2.0" license/type-copyleft-weak "WeakCopyleft"]
   ["GPL with linking exception" license/type-copyleft-weak "WeakCopyleft"]
   ["GPL Classpath" license/type-copyleft-weak "WeakCopyleft"]
   ["GPL v2 or later with classpath exception" license/type-copyleft-weak "WeakCopyleft"]
   ["GNU General Public License v2 or later (GPLv2+)" license/type-copyleft-strong "StrongCopyleft"]
   ["GPLv3" license/type-copyleft-strong "StrongCopyleft"]
   ["BSD-3-Clause OR GPL-2.0" license/type-copyleft-strong "StrongCopyleft"]
   ["GNU General Public License version 3" license/type-copyleft-strong "StrongCopyleft"]
   ["AGPLv3" license/type-copyleft-network "NetworkCopyleft"]
   ["GNU Affero GPL version 3" license/type-copyleft-network "NetworkCopyleft"]
   ["EULA" license/type-other "Other"]])

(deftest test-name->type
  (testing "Get license description by its name"
    (doseq [[license expected description] params-name->type]
      (testing description
        (is (= expected (license/name->type license)) license)))))
