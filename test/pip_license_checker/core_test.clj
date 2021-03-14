(ns pip-license-checker.core-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [pip-license-checker.core :as core]
   [pip-license-checker.file :as file]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.version :as version]))

(def params-validate-args
  [[["--requirements"
     "resources/requirements.txt"
     "django"
     "aiohttp==3.7.1"
     "-r"
     "README.md"]
    {:requirements ["resources/requirements.txt" "README.md"]
     :packages ["django" "aiohttp==3.7.1"]
     :options {:fail #{}
               :pre false
               :with-totals false
               :totals-only false
               :table-headers false}}
    "Normal run"]])

(deftest ^:cli ^:default
  test-validate-args
  (testing "Validating CLI args"
    (doseq [[args expected description] params-validate-args]
      (testing description
        (is (= expected (core/validate-args args)))))))

(def params-get-parsed-requirements
  [[[] [] {} "{}" [] "No input"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}
     {:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
    "Packages and requirements"]
   [["aiohttp==3.7.2"]
    ["test==3.7.2"]
    {:exclude #"aio.*"}
    "{\"info\": {\"license\": \"MIT License\"}}"
    [{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
    "Exclude pattern"]])

(deftest ^:integration ^:request
  test-get-parsed-requirements
  (testing "Integration testing of requirements parsing"
    (doseq [[packages requirements options mock-body expected description]
            params-get-parsed-requirements]
      (testing description
        (with-redefs
         [http/get (constantly {:body mock-body})
          pypi/get-releases (constantly [])
          version/get-version (constantly "3.7.2")
          file/get-requirement-lines (fn [_] requirements)]
          (is
           (= expected
              (vec (core/get-parsed-requiements
                    packages requirements options)))))))))

(def params-get-license-type-totals
  [[[] {} "Empty vector"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
    {"Permissive" 1}
    "Single type"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}
     {:ok? true,
      :requirement {:name "aiokafka", :version "0.6.0"},
      :license {:name "Apache Software License", :desc "Permissive"}}
     {:ok? true,
      :requirement {:name "Synx", :version "0.0.3"},
      :license {:name "Other/Proprietary License", :desc "Other"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.4.2"},
      :license {:name "GPLv3", :desc "Copyleft"}}]
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
               :license {:name license-name :desc license-type}}
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
      :license {:name "MIT License", :desc "Permissive"}}]
    {:fail #{} :with-totals false :totals-only false :table-headers false}
    (str (format core/formatter-license "test:3.7.2" "MIT License" "Permissive") "\n")
    "No headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
    {:fail #{} :with-totals false :totals-only false :table-headers true}
    (str/join
     [(str (format core/formatter-license "Requirement" "License Name" "License Type") "\n")
      (str (format core/formatter-license "test:3.7.2" "MIT License" "Permissive") "\n")])
    "With headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
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
      :license {:name "MIT License", :desc "Permissive"}}]
    {:fail #{} :with-totals false :totals-only true :table-headers false}
    (str/join
     [(str (format core/formatter-totals "Permissive" 1) "\n")])
    "Totals only"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :desc "Permissive"}}]
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
         [core/get-parsed-requiements (constantly mock)
          core/exit #(println (format "Exit code: %s" %))]
          (let [actual (with-out-str (core/process-requirements [] [] options))]
            (is (= expected actual))))))))
