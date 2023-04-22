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

(ns pip-license-checker.version-test
  (:require
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.version :as v]))

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

(def params-parse-number
  [["1" 1 "Ok"]
   [nil 0 "Null -> 0"]])

(deftest test-parse-number
  (testing "Number parsing"
    (doseq [[number expected description] params-parse-number]
      (testing description
        (is (= expected (v/parse-int-or-get-0 number)))))))

(defn enum
  "Enumerate sequence ([idx1 elem1] [id2 elem2] ...)"
  [s]
  (map vector (range) s))

(def params-version
  "This list must be in the correct sorting order"
  [;; Implicit epoch of 0
   "1.0.dev456",
   "1.0a1",
   "1.0a2.dev456",
   "1.0a12.dev456",
   "1.0a12",
   "1.0b1.dev456",
   "1.0b2",
   "1.0b2.post345.dev456",
   "1.0b2.post345",
   "1.0b2-346",
   "1.0c1.dev456",
   "1.0c1",
   "1.0rc2",
   "1.0c3",
   "1.0",
   "1.0.post456.dev34",
   "1.0.post456",
   "1.1.dev1",
   "1.2+123abc",
   "1.2+123abc456",
   "1.2+abc",
   "1.2+abc123",
   "1.2+abc123def",
   "1.2+1234.abc",
   "1.2+123456",
   "1.2.r32+123456",
   "1.2.rev33+123456",
   ;; Explicit epoch of 1
   "1!1.0.dev456",
   "1!1.0a1",
   "1!1.0a2.dev456",
   "1!1.0a12.dev456",
   "1!1.0a12",
   "1!1.0b1.dev456",
   "1!1.0b2",
   "1!1.0b2.post345.dev456",
   "1!1.0b2.post345",
   "1!1.0b2-346",
   "1!1.0c1.dev456",
   "1!1.0c1",
   "1!1.0rc2",
   "1!1.0c3",
   "1!1.0",
   "1!1.0.post456.dev34",
   "1!1.0.post456",
   "1!1.1.dev1",
   "1!1.2+123abc",
   "1!1.2+123abc456",
   "1!1.2+abc",
   "1!1.2+abc123",
   "1!1.2+abc123def",
   "1!1.2+1234.abc",
   "1!1.2+123456",
   "1!1.2.r32+123456",
   "1!1.2.rev33+123456"])

(def params-parse-version
  [["1.0.dev456"
    {:orig "1.0.dev456"
     :epoch 0
     :release [1 0]
     :pre nil
     :post nil
     :dev ["dev" 456]
     :local nil
     :meta nil}
    "Release and dev version"]
   ["1!1.0b2.post345.dev456"
    {:orig "1!1.0b2.post345.dev456"
     :epoch 1
     :release [1 0]
     :pre ["b" 2]
     :post ["post" 345]
     :dev ["dev" 456]
     :local nil
     :meta nil}
    "Release, pre, post and dev version"]
   ["20230213094415!20230213094415.20230213094415.20230213094415.dev20160909030348"
    {:orig "20230213094415!20230213094415.20230213094415.20230213094415.dev20160909030348"
     :epoch 20230213094415
     :release [20230213094415 20230213094415 20230213094415]
     :pre nil
     :post nil
     :dev ["dev" 20160909030348]
     :local nil
     :meta nil}
    "Make sure long numbers handled correctly"]
   ["020230213094415!0020230213094415.00020230213094415.000020230213094415.dev0000020160909030348"
    {:orig "020230213094415!0020230213094415.00020230213094415.000020230213094415.dev0000020160909030348"
     :epoch 20230213094415
     :release [20230213094415 20230213094415 20230213094415]
     :pre nil
     :post nil
     :dev ["dev" 20160909030348]
     :local nil
     :meta nil}
    "Make sure leading zeros parsed as integers, not as octal or hex"]
   ["1.0.dev"
    {:orig "1.0.dev"
     :epoch 0
     :release [1 0]
     :pre nil
     :post nil
     :dev ["dev" 0]
     :local nil
     :meta nil}
    "If dev version cannot be parsed, use 0"]
   ["1.0.dev(exploit-run)"
    nil
    "Make sure no read-string expoit is possible"]])

(deftest test-parse-version
  (testing "Version parsing"
    (doseq [[version expected description] params-parse-version]
      (testing description
        (stest/instrument `v/parse-version)
        (stest/check `v/parse-version)
        (is (= expected (v/parse-version version)))))))

(def params-generators-parse-version (gen/sample sp/version-str-gen 1000))

(deftest test-generators-parse-version
  (testing "Use test.check customer generator for version string parsing"
    (doseq [version params-generators-parse-version]
      (testing version
        (is (map? (v/parse-version version)))))))

(def params-compare-version
  [[(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (< v1-idx v2-idx)]
      [v1-val v2-val])
    "<"
    "v1 is less than v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (<= v1-idx v2-idx)]
      [v1-val v2-val])
    "<="
    "v1 is less than or equal to v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (= v1-idx v2-idx)]
      [v1-val v2-val])
    "=="
    "v1 is equal to v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (= v1-idx v2-idx)]
      [v1-val v2-val])
    "==="
    "v1 is === to v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (not= v1-idx v2-idx)]
      [v1-val v2-val])
    "!="
    "v1 is not equal to v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (> v1-idx v2-idx)]
      [v1-val v2-val])
    ">"
    "v1 is greater than v2"]
   [(for [[v1-idx v1-val] (enum params-version)
          [v2-idx v2-val] (enum params-version)
          :when (>= v1-idx v2-idx)]
      [v1-val v2-val])
    ">="
    "v1 is greater or equal to v2"]])

(deftest test-compare-version
  (testing "Compare unparsed versions"
    (doseq [[versions op-str description] params-compare-version]
      (doseq [[v1 v2] versions]
        (testing (format "desc: %s, v1: %s v2: %s" description v1 v2)
          (let [v1-parsed (v/parse-version v1)
                v2-parsed (v/parse-version v2)
                op (v/get-comparison-op op-str)]
            (is (true? (op v1-parsed v2-parsed)))))))))

(def params-compatible
  [["2.2" "2.5" true "Ok"]
   ["2.2" "3" false "Too high"]
   ["2.2" "1.3" false "Too low"]
   ["1.4.5" "1.4.99" true "Ok"]
   ["1.4.5" "1.5.3" false "Too high"]
   ["1.4.5" "1.2.3" false "Too low"]])

(deftest test-compatible
  (testing "Compatible operator"
    (doseq [[v1 v2 expected description] params-compatible]
      (testing description
        (let [v1-parsed (v/parse-version v1)
              v2-parsed (v/parse-version v2)
              op (v/get-comparison-op "~=")]
          (is (= expected (op v2-parsed v1-parsed))))))))

(def params-specifiers
  [;; Basic cases
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]] "1.9.8" true "Ok"]
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]] "2.0.8" false "Too high"]
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]] "1.2.2" false "Too low"]
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]] "1.5.0" false "Explicit !="]
   ;; Exlcude order comparison for >
   [[[">" "1.7"]] "1.7.post2" false ">: Exclude post version"]
   [[[">" "1.7"]] "1.7+rev.123" false ">: Exclude local version"]
   [[[">=" "1.7"]] "1.7.post2" true "Do not exclude for >="]
   [[[">" "1.7.post1"]] "1.7.post2" true ">: Both versions have post"]
   [[[">" "1.7.post1"]] "1.7.1" true ">: Normal comparison"]
   ;; Exlcude order comparison for <
   [[["<" "4"]] "4.0.0.pre12" false "<: Exclude pre-release"]
   [[["<" "4"]] "4.0.0+rev.33" false "<: Exclude local"]
   [[["<=" "4"]] "4.0.0.a12" true "Do not exclude for <="]
   [[["<" "4.a13"]] "4.0.0.a12" true "<:  Both have pre-releases"]
   [[["<" "4+ubuntu.12"]] "4.0.0+ubuntu.11" true "<: Both have local versions"]])

(deftest test-version-ok?
  (testing "Check is version ok?"
    (doseq [[specs version expected description] params-specifiers]
      (testing description
        (let [specs-parsed
              (vec (map (fn [[op ver]]
                          [(v/get-comparison-op op) (v/parse-version ver)]) specs))
              version-parsed (v/parse-version version)]
          (is (= expected (v/version-ok? specs-parsed version-parsed))))))))

(def params-specifiers-with-versions
  [[[["~=" "1.2.3"]]
    [["0.1.2" nil] ["1.2.4" nil] ["1.2.99" nil] ["1.5.0" nil] ["1.9.8" nil] ["2" nil] ["2.0.1" nil]]
    true
    ["1.2.4" "1.2.99"]
    "Single specifier"]
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]]
    [["0.1.2" nil] ["1.2.3" nil] ["1.5.0" nil] ["1.9.8" nil] ["2" nil] ["2.0.1" nil]]
    true
    ["1.2.3" "1.9.8"]
    "Multiple specifiers"]
   [[["<" "2"]]
    [["1.9.8" nil] ["1.9.9" nil] ["2.0.0.a1" nil] ["2.0.0.a2" nil]]
    true
    ["1.9.8" "1.9.9"]
    "Pre-releases"]
   [[[">" "1.7"]]
    [["1.7.0" nil] ["1.7.0.post1" nil] ["1.7.0.post2" nil] ["1.7.1" nil] ["1.7.2" nil]]
    true
    ["1.7.1" "1.7.2"]
    "Post-releases"]
   [[["<=" "2"]]
    [["1.9.8" nil] ["1.9.9" nil] ["2.0.0.a1" nil] ["2.0.0.a2" nil]]
    false
    ["1.9.8" "1.9.9"]
    "Do not use pre-releases"]
   [[[">=" "1.2.3"] ["<" "2"] ["!=" "1.5.0"]]
    [["2" nil] ["2.0.1" nil]]
    true
    []
    "Empty result"]
   [[]
    [["0.1.2" nil] ["1.2.3" nil] ["1.5.0" nil] ["1.9.8" nil] ["2" nil] ["2.0.1" nil]]
    true
    ["0.1.2" "1.2.3" "1.5.0" "1.9.8" "2" "2.0.1"]
    "Empty specifiers"]
   [[[">" "1.47.2"] ["<" "1.48.1"]]
    [["1.47.2" {:yanked false}]
     ["1.48.0rc1" {:yanked false}]
     ["1.48.0" {:yanked true}]
     ["1.48.1" {:yanked false}]
     ["1.48.2" {:yanked false}]]
    false
    []
    "skip yanked versions for non-exact specifiers"]
   [[[">" "1.47.2"] ["<" "1.48.1"]]
    [["1.47.2" {:yanked false}]
     ["1.48.0rc1" {:yanked false}]
     ["1.48.0" {:yanked true}]
     ["1.48.1" {:yanked false}]
     ["1.48.2" {:yanked false}]]
    true
    ["1.48.0rc1"]
    "skip yanked versions for non-exact specifiers, use pre-release"]
   [[["==" "1.48.0"]]
    [["1.47.2" {:yanked false}]
     ["1.48.0rc1" {:yanked false}]
     ["1.48.0" {:yanked true}]
     ["1.48.1" {:yanked false}]
     ["1.48.2" {:yanked false}]]
    false
    ["1.48.0"]
    "use yanked version for exact equal specifier"]
   [[["===" "1.48.0"]]
    [["1.47.2" {:yanked false}]
     ["1.48.0rc1" {:yanked false}]
     ["1.48.0" {:yanked true}]
     ["1.48.1" {:yanked false}]
     ["1.48.2" {:yanked false}]]
    false
    ["1.48.0"]
    "use yanked version for arbitrary-string equal specifier"]
   [[["==" "0.0.2a32"]]
    [["0.0.1" {:yanked false}]
     ["0.0.2a32" {:yanked false}]
     ["0.0.3" {:yanked false}]]
    false
    ["0.0.2a32"]
    "use pre-release version for exact equal specifier with --no-pre option"]
   [[["===" "0.0.2a32"]]
    [["0.0.1" {:yanked false}]
     ["0.0.2a32" {:yanked false}]
     ["0.0.3" {:yanked false}]]
    false
    ["0.0.2a32"]
    "use pre-release version for arbitrary-string equal specifier with --no-pre option"]])

(deftest test-filter-versions
  (testing "Check versions filtering"
    (doseq [[specs versions pre expected description] params-specifiers-with-versions]
      (testing description
        (let [specs-parsed
              (vec (map (fn [[op ver]]
                          [(v/get-comparison-op op) (v/parse-version ver)]) specs))
              versions-parsed (vec (map #(apply v/parse-version %) versions))
              result
              (vec (map
                    #(:orig %)
                    (v/filter-versions specs-parsed versions-parsed :pre pre)))]
          (is (= expected result)))))))

(def params-sort-versions
  (for [_ (range 10)
        :let [versions-parsed (vec (map #(v/parse-version %) params-version))
              versions-shuffled (shuffle versions-parsed)]
        :when (not= versions-shuffled versions-parsed)]
    [versions-parsed versions-shuffled]))

(deftest test-sort-versions
  (testing "Sorting versions"
    (doseq [[versions-parsed versions-shuffled] params-sort-versions]
      (let [orig-reversed (reverse versions-parsed)
            sorted-asc (v/sort-versions versions-shuffled)
            sorted-desc (v/sort-versions versions-shuffled :order :desc)]
        (is (= versions-parsed sorted-asc))
        (is (= orig-reversed sorted-desc))))))

;; version/compare-letter-version

(def params-compare-letter-version-ok
  [[[1 2] [1 3] -1 "Two vectors"]
   [2 1 1 "Two numbers"]
   [2 [1 2] 1 "Number and vec"]
   [[1 2] 1 -1  "Vec and number"]
   ["string" 1 -1  "String and number, less"]
   ["string" 0 0  "String and number, eq"]])

(deftest test-compare-letter-version-ok
  (testing "Compare letter part"
    (doseq [[a b expected description] params-compare-letter-version-ok]
      (testing description
        (is (= expected (v/compare-letter-version a b)))))))

(def params-compare-letter-version-exc
  [["str 1" "str 2" #"Cannot compare" "Two strings"]
   [#{1 2 3} #"regex" #"Cannot compare" "Set and regex"]])

(deftest test-compare-letter-version-exc
  (testing "Compare letter part is throwing exception"
    (doseq [[a b expected-msg description] params-compare-letter-version-exc]
      (testing description
        (is (thrown-with-msg?
             Exception
             expected-msg (v/compare-letter-version a b)))))))

;; version/version-stable?

(def params-version-stable?
  [["1.7.0" true "Release only"]
   ["1.7.0.post1" true "Post release"]
   ["1.7.0.a1" false "Pre release"]
   ["1.7.0.dev12" false "Dev version"]])

(deftest test-version-stable?
  (testing "Is version stable?"
    (doseq [[version-str expected description] params-version-stable?]
      (testing description
        (let [version (v/parse-version version-str)]
          (is (= expected (v/version-stable? version))))))))

(deftest test-generators-version-stable?
  (testing "Check if autogenerated version is stable"
    (doseq [version-str params-generators-parse-version]
      (testing version-str
        (let [version (v/parse-version version-str)]
          (is (boolean? (v/version-stable? version))))))))

;; filename

(def params-get-dist-version
  [["piny-0.5.2.tar.gz"
    "piny"
    "0.5.2"
    "sdist, project name normalized, filename normalized"]
   ["aiohttp-apispec-1.4.0rc0.tar.gz"
    "AioHTTP_APISpec"
    "1.4.0rc0"
    "sdist, project name denormalized, filename normalized"]
   ["aiohttp_apispec-1.4.0rc0.tar.gz"
    "AioHTTP_APISpec"
    "1.4.0rc0"
    "sdist, project name denormalized, filename normalized"]
   ["aiohttp-2.3.2b2.zip"
    "Aiohttp"
    "2.3.2b2"
    "bdist, project name denormalized, filename normalized"]
   ["Aiohttp-2.0.6-1.zip"
    "AIOHTTP"
    "2.0.6"
    "bdist, project name denormalized, filename denormalized"]
   ["Distutils-1.0.12.manilinux.rpm"
    "distutils"
    "1.0.12"
    "bdist, project name normalized, filename denormalized"]
   ["aiohttp-4.0.0a1-cp36-cp36m-macosx_10_13_x86_64.whl"
    "aiohttp"
    "4.0.0a1"
    "wheel, project name normalized, filename normalized"]
   ["py-torch-1.13.1-cp39-none-macosx_11_0_arm64.whl"
    "PY_Torch"
    "1.13.1"
    "wheel, project name denormalized, filename normalized"]
   ["PY.Torch-1.13.1-cp39-none-macosx_11_0_arm64.whl"
    "PY-Torch"
    "1.13.1"
    "wheel, project name denormalized, filename denormalized"]
   ["aiohttp-apispec-0.9.0rc1.macosx-10.9-x86_64.tar.gz"
    "aiohttp-apispec"
    "0.9.0rc1.macosx-10.9-x86_64"
    "wheel, broken naming convention for binary distribution, in line with `pip index versions aiohttp-apispec` output for pip 22.3.1"]
   ["test-0.1.2-build-python-abi-platform-nonsuchtag-evenmoretags.whl"
    "test"
    "0.1.2"
    "wheel, broken naming convention"]
   ["pyasn1-0.4.8-py3.5.egg"
    "pyasn1"
    "0.4.8"
    "egg, project name normilized, filename normalized"]
   ["iso3166-0.7.zip"
    "iso3166"
    "0.7"
    "zip, project name normilized, filename normalized"]
   ["pyasn1_modules-0.0.1a-py2.4.egg"
    "pyasn1-modules"
    "0.0.1a"
    "tar gzipped, project name normilized, filename denormalized"]
   ["pyasn1-modules-0.0.1a.tar.gz"
    "pyasn1-modules"
    "0.0.1a"
    "tar gzipped, project name normilized, filename normalized"]])

(deftest test-get-dist-version
  (testing "Get distribution version from the filename"
    (doseq [[filename project expected description] params-get-dist-version]
      (testing description
        (is (= expected (v/get-dist-version filename project)))))))
