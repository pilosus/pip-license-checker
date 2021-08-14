(ns pip-license-checker.core-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.core :as core]
   [pip-license-checker.pypi :as pypi]))

(def params-validate-args
  [[["--requirements"
     "resources/requirements.txt"
     "django"
     "aiohttp==3.7.1"
     "-r"
     "README.md"]
    {:requirements ["resources/requirements.txt" "README.md"]
     :external []
     :packages ["django" "aiohttp==3.7.1"]
     :options {:fail #{}
               :pre false
               :external-csv-headers true
               :with-totals false
               :totals-only false
               :table-headers false
               :fails-only false}}
    "Normal run"]])

(deftest ^:cli ^:default
  test-validate-args
  (testing "Validating CLI args"
    (doseq [[args expected description] params-validate-args]
      (testing description
        (is (= expected (core/validate-args args)))))))

(def params-filter-parsed-requirements
  [[[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{"Copyleft"} :fails-only true}
    [{:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "Filter copyleft licenses"]
   [[]
    {:fail #{"Copyleft"} :fails-only true}
    []
    "Filter empty sequence"]
   [[]
    {:fail #{"Copyleft"} :fails-only true}
    []
    "Filter nil sequence"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{"Copyleft"} :fails-only false}
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "fails-only flag is off, do not filter"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    {:fail #{} :fails-only true}
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}]
    "fail flag omitted, do not filter"]])

(deftest ^:integration ^:request
  test-filter-parsed-requirements
  (testing "Integration testing for filtering parsed requirements"
    (doseq [[licenses options expected description]
            params-filter-parsed-requirements]
      (testing description
        (is (= expected (core/filter-parsed-requirements licenses options)))))))

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

(deftest test-print-license-header
  (testing "Printing license table header"
    (let [actual (with-out-str (core/print-license-header))
          expected (str/join
                    [(format core/formatter-license
                             "Requirement"
                             "License Name"
                             "License Type")
                     "\n"])]
      (is (= expected actual)))))

(def params-format-license
  [["test"
    "1.12.3"
    "GPLv3"
    "Copyleft"
    (format core/formatter-license "test:1.12.3" "GPLv3" "Copyleft")
    "Example 1"]
   ["aiohttp"
    "3.7.4.post0"
    "MIT"
    "Permissive"
    (format core/formatter-license "aiohttp:3.7.4.post0" "MIT" "Permissive")
    "Example 2"]])

(deftest test-format-license
  (testing "Printing a line of license table"
    (doseq [[package version license-name license-type expected description] params-format-license]
      (testing description
        (let [license-data
              {:requirement {:name package :version version}
               :license {:name license-name :type license-type}}
              actual (core/format-license license-data)]
          (is (= expected actual)))))))

(deftest test-format-total
  (testing "Formatting total table line"
    (let [expected (format core/formatter-totals "Permissive" 7)
          actual (core/format-total "Permissive" 7)]
      (is (= expected actual)))))

(deftest test-print-totals-header
  (testing "Printing totals table header"
    (let [actual (with-out-str (core/print-totals-header))
          expected (str/join
                    [(format core/formatter-totals "License Type" "Found")
                     "\n"])]
      (is (= expected actual)))))

(def params-process-requirements
  [[[]
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    ""
    "No licenses"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    (str (format core/formatter-license "test:3.7.2" "MIT License" "Permissive") "\n")
    "No headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {:fail #{} :with-totals false :totals-only false :table-headers true}
    (str/join
     [(str (format core/formatter-license "Requirement" "License Name" "License Type") "\n")
      (str (format core/formatter-license "test:3.7.2" "MIT License" "Permissive") "\n")])
    "With headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {:fail #{} :with-totals true :totals-only false :table-headers true}
    (str/join
     [(str (format core/formatter-license "Requirement" "License Name" "License Type") "\n")
      (str (format core/formatter-license "test:3.7.2" "MIT License" "Permissive") "\n")
      "\n"
      (str (format core/formatter-totals "License Type" "Found") "\n")
      (str (format core/formatter-totals "Permissive" 1) "\n")])
    "With totals"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {:fail #{} :with-totals false :totals-only true :table-headers false}
    (str/join
     [(str (format core/formatter-totals "Permissive" 1) "\n")])
    "Totals only"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    {:fail #{"Permissive"} :with-totals false :totals-only true :table-headers false}
    (str/join
     [(str (format core/formatter-totals "Permissive" 1) "\n")
      "Exit code: 1\n"])
    "License matched, exit with non-zero status code"]])

(deftest test-process-requirements
  (testing "Print results"
    (doseq [[mock options expected description] params-process-requirements]
      (testing description
        (with-redefs
         [pypi/get-parsed-requiements (constantly mock)
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
