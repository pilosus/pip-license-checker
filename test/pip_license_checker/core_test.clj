(ns pip-license-checker.core-test
  (:require
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
     :options {:pre false
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
