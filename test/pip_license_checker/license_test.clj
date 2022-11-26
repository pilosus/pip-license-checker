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

(ns pip-license-checker.license-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.data :as d]
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


;; license/license-with-type


(def params-license-with-type
  [["MIT License"
    (d/->License "MIT License" license/type-permissive nil)
    "Permissive"]
   ["Artistic license"
    (d/->License "Artistic license" license/type-permissive nil)
    "Permissive"]
   ["zope public license"
    (d/->License "zope public license" license/type-permissive nil)
    "Permissive"]
   ["WTFPL"
    (d/->License "WTFPL" license/type-permissive nil)
    "Permissive"]
   ["CC0"
    (d/->License "CC0" license/type-permissive nil)
    "Permissive"]
   ["CC-BY-4.0"
    (d/->License "CC-BY-4.0" license/type-permissive nil)
    "Permissive"]
   ["CC BY 4.0"
    (d/->License "CC BY 4.0" license/type-permissive nil)
    "Permissive"]
   ["cc  by 2.0"
    (d/->License "cc  by 2.0" license/type-permissive nil)
    "Permissive"]
   ["ODC BY 1.0"
    (d/->License "ODC BY 1.0" license/type-permissive nil)
    "Permissive"]
   ["ODC-BY-1.0"
    (d/->License "ODC-BY-1.0" license/type-permissive nil)
    "Permissive"]
   ["Open Data Commons Attribution License"
    (d/->License "Open Data Commons Attribution License" license/type-permissive nil)
    "Permissive"]
   ["zlib"
    (d/->License "zlib" license/type-permissive nil)
    "Permissive"]
   ["zlib/libpng License with Acknowledgement"
    (d/->License "zlib/libpng License with Acknowledgement" license/type-permissive nil)
    "Permissive"]
   ["ODbL-1.0"
    (d/->License "ODbL-1.0" license/type-copyleft-weak nil)
    license/type-copyleft-weak
    "WeakCopyleft"]
   ["Open Data Commons Open Database License v1.0"
    (d/->License
     "Open Data Commons Open Database License v1.0"
     license/type-copyleft-weak
     nil)
    "WeakCopyleft"]
   ["Mozilla Public License 2.0"
    (d/->License "Mozilla Public License 2.0" license/type-copyleft-weak nil)
    "WeakCopyleft"]
   ["GPL with linking exception"
    (d/->License "GPL with linking exception" license/type-copyleft-weak nil)
    "WeakCopyleft"]
   ["GPL Classpath"
    (d/->License "GPL Classpath" license/type-copyleft-weak nil)
    "WeakCopyleft"]
   ["GPL v2 or later with classpath exception"
    (d/->License
     "GPL v2 or later with classpath exception"
     license/type-copyleft-weak
     nil)
    "WeakCopyleft"]
   ["GNU General Public License v2 or later (GPLv2+)"
    (d/->License
     "GNU General Public License v2 or later (GPLv2+)"
     license/type-copyleft-strong
     nil)
    "StrongCopyleft"]
   ["GPLv3"
    (d/->License "GPLv3" license/type-copyleft-strong nil)
    "StrongCopyleft"]
   ["BSD-3-Clause OR GPL-2.0"
    (d/->License "BSD-3-Clause OR GPL-2.0" license/type-copyleft-strong nil)
    "StrongCopyleft"]
   ["GNU General Public License version 3"
    (d/->License
     "GNU General Public License version 3"
     license/type-copyleft-strong
     nil)
    "StrongCopyleft"]
   ["AGPLv3"
    (d/->License "AGPLv3" license/type-copyleft-network nil)
    license/type-copyleft-network
    "NetworkCopyleft"]
   ["GNU Affero GPL version 3"
    (d/->License "GNU Affero GPL version 3" license/type-copyleft-network nil)
    "NetworkCopyleft"]
   ["zlib/whatever-new"
    (d/->License "zlib/whatever-new" license/type-other nil)
    "Other"]
   ["CC-BY-SA"
    (d/->License "CC-BY-SA" license/type-other nil)
    "Other"]
   ["CC BY-NC"
    (d/->License "CC BY-NC" license/type-other nil)
    "Other"]
   ["CC BY-ND"
    (d/->License "CC BY-ND" license/type-other nil)
    "Other"]
   ["CC BY-NC-ND"
    (d/->License "CC BY-NC-ND" license/type-other nil)
    "Other"]
   ["CC BY-NC-SA-4.0"
    (d/->License "CC BY-NC-SA-4.0" license/type-other nil)
    "Other"]
   ["EULA"
    (d/->License "EULA" license/type-other nil)
    "Other"]
   [nil
    (d/->License license/name-error license/type-error nil)
    "Exception catched"]])

(deftest test-license-with-type
  (testing "Get license description by its name"
    (doseq [[license expected description] params-license-with-type]
      (testing description
        (is (= expected (license/license-with-type license)) license)))))
