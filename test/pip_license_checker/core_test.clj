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

(ns pip-license-checker.core-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.core :as core]
   [pip-license-checker.external :as external]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.report :as report]))

(def params-validate-args
  [[["--requirements"
     "resources/requirements.txt"]
    {:requirements ["resources/requirements.txt"]
     :external []
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "Requirements only run"]
   [["django"
     "aiohttp==3.7.1"]
    {:requirements []
     :external []
     :packages ["django" "aiohttp==3.7.1"]
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "Packages only"]
   [["--external"
     "resources/external.csv"]
    {:requirements []
     :external ["resources/external.csv"]
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "External only"]
   [["--requirements"
     "resources/requirements.txt"
     "django"
     "aiohttp==3.7.1"
     "-r"
     "README.md"
     "--external"
     "resources/external.csv"]
    {:requirements ["resources/requirements.txt" "README.md"]
     :external ["resources/external.csv"]
     :packages ["django" "aiohttp==3.7.1"]
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "Requirements, packages and externals"]
   [["--requirements"
     "resources/requirements.txt"
     "--rate-limits"
     "25/60000"]
    {:requirements ["resources/requirements.txt"]
     :external []
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 25 :millis 60000}}}
    "Requirements, rate limits"]
   [["--requirements"
     "resources/requirements.github.txt"
     "--rate-limits"
     "25/60000"
     "--github-token"
     "github-oauth-bearer-token"]
    {:requirements ["resources/requirements.github.txt"]
     :external []
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :formatter report/table-formatter
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token "github-oauth-bearer-token"
               :rate-limits {:requests 25 :millis 60000}}}
    "GitHub OAuth token"]
   [["--external"
     "resources/external.cocoapods"
     "--external-options"
     "{:skip-header false :skip-footer true :int-opt 42 :str-opt \"str-val\"}"
     "--external-format"
     "cocoapods"
     "--with-totals"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header false :skip-footer true :int-opt 42 :str-opt "str-val"}
               :formatter report/table-formatter
               :with-totals true
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "Externals with format and options specified"]
   [["--external"
     "resources/external.cocoapods"
     "--external-format"
     "cocoapods"
     "--formatter"
     "%-50s %-50s %-30s"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header true, :skip-footer true}
               :formatter "%-50s %-50s %-30s"
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false
               :github-token nil
               :rate-limits {:requests 120 :millis 60000}}}
    "Formatter string"]
   [["--help"]
    {:exit-message "placeholder" :ok? true}
    "Help run"]])

(deftest ^:cli ^:default
  test-validate-args
  (testing "Validating CLI args"
    (doseq [[args expected description] params-validate-args]
      (testing description
        (with-redefs
         [core/usage (constantly "placeholder")]
          (is (= expected (core/validate-args args))))))))

(def params-get-license-type-totals
  [[[] {} "Empty vector"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {"Permissive" 1}
    "Single type"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiokafka", :version "0.6.0"},
      :license {:name "Apache Software License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "Synx", :version "0.0.3"},
      :license {:name "Other/Proprietary License", :type "Other"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.4.2"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {"Copyleft" 1 "Permissive" 2 "Other" 1}
    "Multiple types"]])

(deftest
  test-get-license-type-totals
  (testing "Count totals"
    (doseq [[licenses expected description]
            params-get-license-type-totals]
      (testing description
        (is
         (= expected (core/get-license-type-totals licenses)))))))

(def params-process-requirements
  [[[]
    []
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    ""
    "No licenses"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    (str (format report/table-formatter "test:3.7.2" "MIT License" "Permissive") "\n")
    "No headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    {:fail #{} :with-totals false :totals-only false :table-headers true}
    (str/join
     [(str (format report/table-formatter "Requirement" "License Name" "License Type") "\n")
      (str (format report/table-formatter "test:3.7.2" "MIT License" "Permissive") "\n")])
    "With headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    {:fail #{} :with-totals true :totals-only false :table-headers true}
    (str/join
     [(str (format report/table-formatter "Requirement" "License Name" "License Type") "\n")
      (str (format report/table-formatter "test:3.7.2" "MIT License" "Permissive") "\n")
      "\n"
      (str (format report/totals-formatter "License Type" "Found") "\n")
      (str (format report/totals-formatter "Permissive" 1) "\n")])
    "With totals"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    {:fail #{} :with-totals false :totals-only true :table-headers false}
    (str/join
     [(str (format report/totals-formatter "Permissive" 1) "\n")])
    "Totals only"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    [{:ok? true,
      :requirement {:name "another", :version "0.1.2"},
      :license {:name "BSD License", :type "Permissive"}}]
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    (str/join
     [(str (format report/table-formatter "test:3.7.2" "MIT License" "Permissive") "\n")
      (str (format report/table-formatter "another:0.1.2" "BSD License" "Permissive") "\n")])
    "Requirements and external file"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    {:fail #{"Permissive"} :with-totals false :totals-only true :table-headers false}
    (str/join
     [(str (format report/totals-formatter "Permissive" 1) "\n")
      "Exit code: 1\n"])
    "License matched, exit with non-zero status code"]])

(deftest test-process-requirements
  (testing "Print results"
    (doseq [[mock-pypi mock-external options expected description] params-process-requirements]
      (testing description
        (with-redefs
         [pypi/get-parsed-requiements (constantly mock-pypi)
          external/get-parsed-requiements (constantly mock-external)
          core/exit #(println (format "Exit code: %s" %))]
          (let [actual (with-out-str (core/process-requirements [] [] [] options))]
            (is (= expected actual))))))))

(def params-options
  [[{:requirements ["test1" "test2"] :totals-only true :fail #{}}
    {:totals-only true :fail #{}}
    "Requirements removed from options"]
   [{:requirements ["test1" "test2"]
     :fails-only true
     :fail #{"Copyleft" "Other"}}
    {:fails-only true
     :fail #{"WeakCopyleft" "StrongCopyleft" "NetworkCopyleft" "Other"}}
    "Copyleft Extended"]
   [{:requirements ["test1" "test2"]
     :fail #{"WeakCopyleft" "Other"}}
    {:fail #{"WeakCopyleft" "Other"}}
    "Other copyleft types left unextended"]])

(deftest test-post-process-options
  (testing "Post process options"
    (doseq [[options expected description] params-options]
      (testing description
        (is (= expected (core/post-process-options options)))))))

(def params-parse-rate-limits
  [["120/60000" {:requests 120 :millis 60000} "Correct format"]
   ["25" {:requests 25 :millis nil} "Millis absent"]
   ["50/1000/120/60000" {:requests 50 :millis 1000} "Excessive args ignored"]])

(deftest test-parse-rate-limits
  (testing "Parse rate limits CLI option"
    (doseq [[limits-str expected description] params-parse-rate-limits]
      (testing description
        (is (= expected (core/parse-rate-limits limits-str)))))))

(def params-validate-rate-limits
  [[{:requests 120 :millis 60000} true "All data valid"]
   [{:requests 25 :millis nil} false "Millis absent"]
   [{:requests 0 :millis 60000} false "Requests 0"]
   [{:requests 10 :millis -10} false "Millis negative"]])

(deftest test-validate-rate-limits
  (testing "Validate rate limits CLI option"
    (doseq [[limits-str expected description] params-validate-rate-limits]
      (testing description
        (is (= expected (core/validate-rate-limits limits-str)))))))
