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

(ns pip-license-checker.filters-test
  (:require
   [clojure.spec.gen.alpha :as gen]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.filters :as filters]))

;; filters/remove-requirements-internal-rules

(def params-remove-requirements-internal-rules
  [[[] [] "No requirements"]
   [nil [] "Requirements is nil"]
   [["  # this is a comment" "django"] ["django"] "Comments"]
   [["-r resources/requirements.txt" "django"] ["django"] "Option 1"]
   [[" --requirement resources/requirements.txt" "django"] ["django"] "Option 2"]
   [["" "django"] ["django"] "Blank line 1"]
   [[" \n" "django"] ["django"] "Blank line 2"]
   [["http://example.com/package.whl" "django"] ["django"] "Url 1"]
   [[" https://example.com/package.whl" "django"] ["django"] "Url 2"]])

(deftest test-remove-requirements-internal-rules
  (testing "Removing lines from requirements with internal rules"
    (doseq [[requirements expected description]
            params-remove-requirements-internal-rules]
      (testing description
        (is
         (= expected
            (filters/remove-requirements-internal-rules requirements)))))))

(def params-generators-requirements (gen/sample sp/requirements-str-gen 100))
(def params-generators-pattern (map re-pattern (gen/sample sp/non-empty-str-gen 100)))

(deftest test-generators-remove-requirements-internal-rules
  (testing "Generative test using test.check for internal requirements filtering"
    (doseq [requirements params-generators-requirements]
      (testing requirements
        (let [result (filters/remove-requirements-internal-rules requirements)]
          (is (seq? result)))))))


;; filters/remove-requirements-user-rules


(def params-remove-requirements-user-rules
  [[[] #"test" [] "No requirements"]
   [nil #"test" [] "Requirements is nil"]
   [["aiohttp==3.7.2" nil "django" nil] #"aio.*" [nil "django" nil] "Do nothing with null values"]
   [["aiohttp==3.7.2" "django"] #"aio.*" ["django"] "Pattern 1"]
   [["aiohttp==3.7.2" "django"] #".*" [] "Pattern 2"]])

(deftest test-remove-requirements-user-rules
  (testing "Removing lines from requirements with user-defined rules"
    (doseq [[requirements pattern expected description]
            params-remove-requirements-user-rules]
      (testing description
        (is
         (= expected
            (filters/remove-requirements-user-rules pattern requirements)))))))

(deftest test-generators-remove-requirements-user-rules
  (testing "Generative test using test.check for user requirements filtering"
    (doseq [[pattern requirements]
            (map
             list
             params-generators-pattern
             params-generators-requirements)]
      (testing (str pattern requirements)
        (let [result
              (filters/remove-requirements-user-rules pattern requirements)]
          (is (seq? result)))))))


;; filters/remove-requiment-maps-user-rules


(def params-remove-requiment-maps-user-rules
  [[[{:requirement {:name "test" :version "1.2.3"} :license {:name "MIT" :type "Permissive"}}]
    #"wut"
    [{:requirement {:name "test" :version "1.2.3"} :license {:name "MIT" :type "Permissive"}}]
    "No matches"]
   [[{:requirement {:name "test" :version "1.2.3"} :license {:name "MIT" :type "Permissive"}}]
    #"(?i).*test.*"
    []
    "Match"]
   [[{:requirement {:name "test" :version "1.2.3"} :license {:name "MIT" :type "Permissive"}}
     {:requirement {:name nil :version nil} :license {:name nil :type "Error"}}
     {:requirement {:name "another" :version "21.04"} :license {:name "GPLv2+" :type "StrongCopyleft"}}]
    #"^test$"
    [{:requirement {:name nil :version nil} :license {:name nil :type "Error"}}
     {:requirement {:name "another" :version "21.04"} :license {:name "GPLv2+" :type "StrongCopyleft"}}]
    "Nil is ignored"]])

(deftest test-remove-requiment-maps-user-rules
  (testing "Removing lines from requirements with user-defined rules"
    (doseq [[packages pattern expected description]
            params-remove-requiment-maps-user-rules]
      (testing description
        (is
         (= expected
            (filters/remove-requiment-maps-user-rules pattern packages)))))))


;; filters/sanitize-requirement


(def params-sanitize-requirement
  [["  hello == 1.2.3" "hello==1.2.3" "Whitespaces"]
   ["  hello == 1.2.3  #comment" "hello==1.2.3" "Comment"]
   ["  hello == 1.2.3 ; python_version > '3.6'" "hello==1.2.3" "Modifiers"]])

(deftest test-sanitize-requirement
  (testing "Sanitizing requirement string"
    (doseq [[requirement expected description] params-sanitize-requirement]
      (testing description
        (is (= expected (filters/sanitize-requirement requirement)))))))


;; filters/remove-licenses


(def params-remove-licenses
  [[[{:license {:name "MIT" :type "Permissive"}}
     {:license {:name "GPLv2+" :type "StrongCopyleft"}}]
    {}
    [{:license {:name "MIT" :type "Permissive"}}
     {:license {:name "GPLv2+" :type "StrongCopyleft"}}]
    "No options, nothing removed"]
   [[{:license {:name "MIT" :type "Permissive"}}
     {:license {:name "GPLv2+" :type "StrongCopyleft"}}]
    {:exclude-license #"(?i)^MIT.*"}
    [{:license {:name "GPLv2+" :type "StrongCopyleft"}}]
    "Skip MIT"]
   [[{:license {:name nil :type "Error"}}
     {:license {:name "GPLv2+" :type "StrongCopyleft"}}]
    {:exclude-license #"GPL.*"}
    [{:license {:name nil :type "Error"}}]
    "Null value ignored"]])

(deftest test-remove-licenses
  (testing "Requirement string to a map of name and specifiers"
    (doseq [[licenses options expected description] params-remove-licenses]
      (testing description
        (is (= expected (filters/remove-licenses options licenses)))))))


;; filters/filter-parsed-requirements


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
    "fail flag omitted, do not filter"]
   [[{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}
     {:ok? true,
      :requirement {:name "aiostream", :version "0.1.0"},
      :license {:name "GPLv3", :type "Copyleft"}}
     {:ok? true,
      :requirement {:name "whatever", :version "10.1.2"},
      :license {:name "GPL version 2", :type "Copyleft"}}
     {:ok? true,
      :requirement {:name "yet_another_package", :version "21.04"},
      :license {:name "GNU General Public License (GPLv2+)", :type "Copyleft"}}]
    {:exclude-license #"(?i)(?:^GPL.*|gnu general public license.*)"}
    [{:ok? true,
      :requirement {:name "aiohttp", :version "3.7.2"},
      :license {:name "MIT License", :type "Permissive"}}]
    "Exclude license"]])

(deftest ^:integration ^:request
  test-filter-parsed-requirements
  (testing "Integration testing for filtering parsed requirements"
    (doseq [[licenses options expected description]
            params-filter-parsed-requirements]
      (testing description
        (is (= expected (filters/filter-parsed-requirements licenses options)))))))
