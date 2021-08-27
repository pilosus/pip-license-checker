(ns pip-license-checker.license-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.license :as license]))


;; license/is-type-valid?


(def params-is-type-valid?
  [["NetworkCopyleft" true]
   ["StrongCopyleft" true]
   ["WeakCopyleft" true]
   ["Copyleft" true]
   ["Permissive" true]
   ["Other" true]
   ["EULA" false]
   ["CopyleftWhateverYouCan" false]])

(deftest test-is-type-valid?
  (testing "Test checking valid license types"
    (doseq [[type expected] params-is-type-valid?]
      (testing type
        (is (= expected (license/is-type-valid? type)))))))


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
   ["CC-BY-4.0" license/type-permissive "Permissive"]
   ["CC BY 4.0" license/type-permissive "Permissive"]
   ["cc  by 2.0" license/type-permissive "Permissive"]
   ["ODC BY 1.0" license/type-permissive "Permissive"]
   ["ODC-BY-1.0" license/type-permissive "Permissive"]
   ["Open Data Commons Attribution License" license/type-permissive "Permissive"]
   ["ODbL-1.0" license/type-copyleft-weak "WeakCopyleft"]
   ["Open Data Commons Open Database License v1.0" license/type-copyleft-weak "WeakCopyleft"]
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
   ["CC-BY-SA" license/type-other "Other"]
   ["CC BY-NC" license/type-other "Other"]
   ["CC BY-ND" license/type-other "Other"]
   ["CC BY-NC-ND" license/type-other "Other"]
   ["CC BY-NC-SA-4.0" license/type-other "Other"]
   ["EULA" license/type-other "Other"]
   [nil license/type-error "Exception catched"]])

(deftest test-name->type
  (testing "Get license description by its name"
    (doseq [[license expected description] params-name->type]
      (testing description
        (is (= expected (license/name->type license)) license)))))
