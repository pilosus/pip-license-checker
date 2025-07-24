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

(ns pip-license-checker.license-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.license :as license]
   [pip-license-checker.spec :as sp]))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

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
    (s/assert
     ::sp/license
     {:name "MIT License" :type license/type-permissive :logs nil})
    "Permissive"]
   ["Artistic license"
    (s/assert
     ::sp/license
     {:name "Artistic license" :type license/type-permissive :logs nil})
    "Permissive"]
   ["zope public license"
    (s/assert
     ::sp/license
     {:name "zope public license" :type license/type-permissive :logs nil})
    "Permissive"]
   ["WTFPL"
    (s/assert
     ::sp/license
     {:name "WTFPL" :type license/type-permissive :logs nil})
    "Permissive"]
   ["PSF-2.0"
    (s/assert
     ::sp/license
     {:name "PSF-2.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["CC0"
    (s/assert
     ::sp/license
     {:name "CC0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["CC-BY-4.0"
    (s/assert
     ::sp/license
     {:name "CC-BY-4.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["CC BY 4.0"
    (s/assert
     ::sp/license
     {:name "CC BY 4.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["cc  by 2.0"
    (s/assert
     ::sp/license
     {:name "cc  by 2.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["ODC BY 1.0"
    (s/assert
     ::sp/license
     {:name "ODC BY 1.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["ODC-BY-1.0"
    (s/assert
     ::sp/license
     {:name "ODC-BY-1.0" :type license/type-permissive :logs nil})
    "Permissive"]
   ["Open Data Commons Attribution License"
    (s/assert
     ::sp/license
     {:name "Open Data Commons Attribution License"
      :type license/type-permissive
      :logs nil})
    "Permissive"]
   ["zlib"
    (s/assert
     ::sp/license
     {:name "zlib" :type license/type-permissive :logs nil})
    "Permissive"]
   ["zlib/libpng License with Acknowledgement"
    (s/assert
     ::sp/license
     {:name "zlib/libpng License with Acknowledgement"
      :type license/type-permissive
      :logs nil})
    "Permissive"]
   ["ODbL-1.0"
    (s/assert
     ::sp/license
     {:name "ODbL-1.0" :type license/type-copyleft-weak :logs nil})
    license/type-copyleft-weak
    "WeakCopyleft"]
   ["Open Data Commons Open Database License v1.0"
    (s/assert
     ::sp/license
     {:name "Open Data Commons Open Database License v1.0"
      :type license/type-copyleft-weak
      :logs nil})
    "WeakCopyleft"]
   ["Mozilla Public License 2.0"
    (s/assert
     ::sp/license
     {:name "Mozilla Public License 2.0" :type license/type-copyleft-weak :logs nil})
    "WeakCopyleft"]
   ["GPL with linking exception"
    (s/assert
     ::sp/license
     {:name "GPL with linking exception" :type license/type-copyleft-weak :logs nil})
    "WeakCopyleft"]
   ["GPL Classpath"
    (s/assert
     ::sp/license
     {:name "GPL Classpath" :type license/type-copyleft-weak :logs nil})
    "WeakCopyleft"]
   ["GPL v2 or later with classpath exception"
    (s/assert
     ::sp/license
     {:name "GPL v2 or later with classpath exception"
      :type license/type-copyleft-weak
      :logs nil})
    "WeakCopyleft"]
   ["GNU General Public License v2 or later (GPLv2+)"
    (s/assert
     ::sp/license
     {:name "GNU General Public License v2 or later (GPLv2+)"
      :type license/type-copyleft-strong
      :logs nil})
    "StrongCopyleft"]
   ["GPLv3"
    (s/assert
     ::sp/license
     {:name "GPLv3" :type license/type-copyleft-strong :logs nil})
    "StrongCopyleft"]
   ["BSD-3-Clause OR GPL-2.0"
    (s/assert
     ::sp/license
     {:name "BSD-3-Clause OR GPL-2.0" :type license/type-copyleft-strong :logs nil})
    "StrongCopyleft"]
   ["GNU General Public License version 3"
    (s/assert
     ::sp/license
     {:name "GNU General Public License version 3"
      :type license/type-copyleft-strong
      :logs nil})
    "StrongCopyleft"]
   ["AGPLv3"
    (s/assert
     ::sp/license
     {:name "AGPLv3" :type license/type-copyleft-network :logs nil})
    license/type-copyleft-network
    "NetworkCopyleft"]
   ["GNU Affero GPL version 3"
    (s/assert
     ::sp/license
     {:name "GNU Affero GPL version 3" :type license/type-copyleft-network :logs nil})
    "NetworkCopyleft"]
   ["zlib/whatever-new"
    (s/assert
     ::sp/license
     {:name "zlib/whatever-new" :type license/type-other :logs nil})
    "Other"]
   ["CC-BY-SA"
    (s/assert
     ::sp/license
     {:name "CC-BY-SA" :type license/type-other :logs nil})
    "Other"]
   ["CC BY-NC"
    (s/assert
     ::sp/license
     {:name "CC BY-NC" :type license/type-other :logs nil})
    "Other"]
   ["CC BY-ND"
    (s/assert
     ::sp/license
     {:name "CC BY-ND" :type license/type-other :logs nil})
    "Other"]
   ["CC BY-NC-ND"
    (s/assert
     ::sp/license
     {:name "CC BY-NC-ND" :type license/type-other :logs nil})
    "Other"]
   ["CC BY-NC-SA-4.0"
    (s/assert
     ::sp/license
     {:name "CC BY-NC-SA-4.0" :type license/type-other :logs nil})
    "Other"]
   ["EULA"
    (s/assert
     ::sp/license
     {:name "EULA" :type license/type-other :logs nil})
    "Other"]
   [nil
    (s/assert
     ::sp/license
     {:name license/name-error :type license/type-error :logs nil})
    "Exception catched"]])

(deftest test-license-with-type
  (testing "Get license description by its name"
    (doseq [[license expected description] params-license-with-type]
      (testing description
        (is (= expected (license/license-with-type license)) license)))))
