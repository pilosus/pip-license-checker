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

(ns pip-license-checker.core-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.core :as core]
   [pip-license-checker.external :as external]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.report :as report]
))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

(def params-validate-args
  [[["--requirements"
     "resources/requirements.txt"]
    {:requirements ["resources/requirements.txt"]
     :external []
     :packages []
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Requirements only run"]
   [["django"
     "aiohttp==3.7.1"]
    {:requirements []
     :external []
     :packages ["django" "aiohttp==3.7.1"]
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Packages only"]
   [["--external"
     "resources/external.csv"]
    {:requirements []
     :external ["resources/external.csv"]
     :packages []
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
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
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Requirements, packages and externals"]
   [["--requirements"
     "resources/requirements.txt"
     "--rate-limits"
     "25/60000"]
    {:requirements ["resources/requirements.txt"]
     :external []
     :packages []
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
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
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "csv"
               :external-options external/default-options
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token "github-oauth-bearer-token"
               :parallel true
               :exit true
               :rate-limits {:requests 25 :millis 60000}}}
    "GitHub OAuth token"]
   [["--external"
     "resources/external.cocoapods"
     "--external-options"
     "{:skip-header false :skip-footer true :int-opt 42 :str-opt \"str-val\"}"
     "--external-format"
     "cocoapods"
     "--totals"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header false :skip-footer true :int-opt 42 :str-opt "str-val"}
               :report-format report/format-stdout
               :formatter report/report-formatter
               :totals true
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
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
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header true, :skip-footer true}
               :report-format report/format-stdout
               :formatter "%-50s %-50s %-30s"
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Formatter string"]
   [["--external"
     "resources/external.cocoapods"
     "--external-format"
     "cocoapods"
     "--report-format"
     "json-pretty"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:verbose 0
               :fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header true, :skip-footer true}
               :report-format report/format-json-pretty
               :formatter report/report-formatter
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Report format"]
   [["-v"
     "--external"
     "resources/external.cocoapods"
     "--external-format"
     "cocoapods"
     "--formatter"
     "%-50s %-50s %-30s"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:verbose 1
               :fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header true, :skip-footer true}
               :report-format report/format-stdout
               :formatter "%-50s %-50s %-30s"
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Verbose output"]
   [["-vvv"
     "--external"
     "resources/external.cocoapods"
     "--external-format"
     "cocoapods"
     "--formatter"
     "%-50s %-50s %-30s"]
    {:requirements []
     :external ["resources/external.cocoapods"]
     :packages []
     :options {:verbose 3
               :fail #{}
               :pre false
               :external-format "cocoapods"
               :external-options {:skip-header true, :skip-footer true}
               :report-format report/format-stdout
               :formatter "%-50s %-50s %-30s"
               :totals false
               :totals-only false
               :headers false
               :fails-only false
               :github-token nil
               :parallel true
               :exit true
               :rate-limits {:requests 120 :millis 60000}}}
    "Very verbose output"]
   [["--help"]
    {:exit-message "placeholder" :ok? true}
    "Help run"]
   [["--no-parallel"]
    {:exit-message "placeholder"}
    "No packages, no requirements, no external files"]
   [["-r" "--resources/requirements.txt"
     "--formatter" "%s %s %s %s %s %d"]
    {:exit-message "The following errors occurred while parsing command arguments:\nFailed to validate \"-r --resources/requirements.txt\": Requirements file does not exist\nFailed to validate \"--formatter %s %s %s %s %s %d\": Invalid formatter string. Expected a printf-style formatter to cover 4 columns of string data, e.g. '%-35s %-55s %-20s %-40s'"}
    "Invalid option"]])

(deftest ^:cli ^:default
  test-validate-args
  (testing "Validating CLI args"
    (doseq [[args expected description] params-validate-args]
      (testing description
        (with-redefs
         [core/usage (constantly "placeholder")]
          (is (= expected (core/validate-args args))))))))

(def params-get-totals
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
  test-get-totals
  (testing "Count totals"
    (doseq [[licenses expected description]
            params-get-totals]
      (testing description
        (is
         (= expected (core/get-totals licenses)))))))

(def params-main
  [[[]
    []
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--no-totals"
     "--no-totals-only"
     "--no-headers"
     "--no-parallel"
     "--no-exit"]
    ""
    "No licenses"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--no-totals"
     "--no-totals-only"
     "--no-headers"
     "--no-parallel"
     "--no-exit"]
    (str (format report/report-formatter "test:3.7.2" "MIT License" "Permissive" "") "\n")
    "No headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--no-totals"
     "--no-totals-only"
     "--headers"
     "--no-parallel"
     "--no-exit"]
    (str/join
     [(str (format report/report-formatter "Dependency" "License Name" "License Type" "Misc") "\n")
      (str (format report/report-formatter "test:3.7.2" "MIT License" "Permissive" "") "\n")])
    "With headers"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--totals"
     "--no-totals-only"
     "--headers"
     "--no-parallel"
     "--no-exit"]
    (str/join
     [(str (format report/report-formatter "Dependency" "License Name" "License Type" "Misc") "\n")
      (str (format report/report-formatter "test:3.7.2" "MIT License" "Permissive" "") "\n")
      "\n"
      (str (format (report/get-totals-fmt) "License Type" "Found") "\n")
      (str (format (report/get-totals-fmt) "Permissive" 1) "\n")])
    "With totals"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    []
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--no-totals"
     "--totals-only"
     "--no-headers"
     "--no-parallel"
     "--no-exit"]
    (str/join
     [(str (format (report/get-totals-fmt) "Permissive" 1) "\n")])
    "Totals only"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    [{:ok? true,
      :requirement {:name "another", :version "0.1.2"},
      :license {:name "BSD License", :type "Permissive"}}]
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--no-totals"
     "--no-totals-only"
     "--no-headers"
     "--no-parallel"
     "--no-exit"]
    (str/join
     [(str (format report/report-formatter "test:3.7.2" "MIT License" "Permissive" "") "\n")
      (str (format report/report-formatter "another:0.1.2" "BSD License" "Permissive" "") "\n")])
    "Requirements and external file"]
   [[{:ok? true,
      :requirement {:name "test", :version "3.7.2"},
      :license {:name "Error"
                :type "Error"}
      :logs [{:level :error
              :name "PyPI::version"
              :message "Not found"}
             {:level :debug
              :name "clj-http"
              :message "TLS Handshake"}
             {:level :info
              :name "GitHub::version"
              :message "Fallback to GitHub API"}]}]
    [{:ok? true,
      :requirement {:name "another" :version "0.1.2"},
      :license {:name "BSD License"
                :type "Permissive"}
      :logs [{:level :info
              :name "CSV::loader"
              :message "File corrupted"}]}]
    ["-r" "resources/requirements.txt"
     "-x" "resources/external.csv"
     "--verbose"
     "--verbose"
     "--no-totals"
     "--no-totals-only"
     "--no-headers"
     "--no-parallel"
     "--no-exit"]
    (str/join
     [(str (format "%-35s %-55s %-20s %-40s"
                   "test:3.7.2"
                   "Error"
                   "Error"
                   "Error: PyPI::version Not found\nInfo: GitHub::version Fallback to GitHub API") "\n")
      (str (format "%-35s %-55s %-20s %-40s"
                   "another:0.1.2"
                   "BSD License"
                   "Permissive"
                   "Info: CSV::loader File corrupted        ") "\n")])
    "Verbosity info level"]])

(deftest test-main
  (testing "main function"
    (doseq [[mock-pypi mock-external args expected description] params-main]
      (testing description
        (with-redefs
         [pypi/get-parsed-deps (constantly mock-pypi)
          external/get-parsed-deps (constantly mock-external)]
          (let [actual (with-out-str (apply core/-main args))]
            (is (= expected actual))))))))

(def params-main-exit
  [["error" false "Non-zero exit code"]
   ["error" true "Zero exit code"]])

(deftest test-main-exit
  (testing "exit"
    (doseq [[exit-message ok? description] params-main-exit]
      (testing description
        (with-redefs
         [core/validate-args (constantly {:exit-message exit-message :ok? ok?})
          core/exit (constantly true)]
          (is (= nil (apply core/-main []))))))))

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
    "Other copyleft types left unextended"]
   [{:requirements ["test1" "test2"]
     :with-totals true
     :totals false}
    {:totals true :fail nil}
    "deprecated --with-totals backward compatible with --totals and still has priority"]
   [{:requirements ["test1" "test2"]
     :with-totals nil
     :totals false}
    {:totals false :fail nil}
    "deprecated --with-totals defaults ignored in favour of --totals"]
   [{:requirements ["test1" "test2"]
     :with-totals nil
     :totals true}
    {:totals true :fail nil}
    "deprecated --with-totals defaults ignored in favour of --totals for all values"]
   [{:requirements ["test1" "test2"]
     :table-headers true
     :headers false}
    {:headers true :fail nil}
    "deprecated --table-headers backward compatible with --headers and still has priority"]
   [{:requirements ["test1" "test2"]
     :table-headers nil
     :headers false}
    {:headers false :fail nil}
    "deprecated --table-headers defaults ignored in favour of --headers"]
   [{:requirements ["test1" "test2"]
     :table-headers nil
     :headers true}
    {:headers true :fail nil}
    "deprecated --table-headers defaults ignored in favour of --headers for all values"]])

(deftest test-update-options
  (testing "Post process options"
    (doseq [[options expected description] params-options]
      (testing description
        (is (= expected (core/update-options options)))))))

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

(def params-shutdown
  [[{:fails true}
    {:exit true :parallel true}
    "pool\nexit\n"
    "Stop threads pool, exit"]
   [{:fails false}
    {:exit true :parallel true}
    "pool\n"
    "Stop threads pool, do not exit"]
   [{:fails false}
    {:exit false :parallel true}
    ""
    "Do not shutdown"]])

(deftest test-shutdown
  (testing "Shutdown function"
    (doseq [[report options expected description] params-shutdown]
      (testing description
        (with-redefs [core/exit (fn [& _] (println "exit"))
                      shutdown-agents (fn [& _] (println "pool"))]
          (is (= expected (with-out-str (core/shutdown report options)))))))))
