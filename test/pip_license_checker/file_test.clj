(ns pip-license-checker.file-test
  (:require [clojure.test :refer :all]
            [pip-license-checker.file :as file]
            [pip-license-checker.core :as core]))


(def dependencies
  [["SomeProject", ["SomeProject"] "Latest version"]
   ["SomeProject == 1.3", ["SomeProject", "1.3"] "Equal to"]
   ["SomeProject~=1.4.2", ["SomeProject", "1.4.2"] "Almost equal to"]
   ["SomeProject[foo, bar] >= 1.5.6", ["SomeProject", "1.5.6"] "Extra dependencies"]
   ["SomeProject >=1.2,<2.0" ["SomeProject" "1.2"] "Lower and upper versions set"]
   ["SomeProject == 1.3.*", ["SomeProject", "1.3"] "Wildcard patch version"]
   ["SomeProject != 1.3", ["SomeProject", "1.3"] "Not equal"]  ;; FIXME
   ["SomeProject == 1.3.4 ; python_version < '2.7'", ["SomeProject", "1.3.4"] "Environment markers"]
   ["SomeProject == 1.3.4 @ file:///somewhere/", ["SomeProject", "1.3.4"] "Direct reference"]])


(def requirements-lines
  [["ok-dep=~0.1.2"
    "yet_another_ok_package>=1.2.7,<2"
   "https://example.com/package_3.0.3.dev1820+49a8884-cp34-none-win_amd64.whl"
   "#aiohttp==3.7.2"
   "-r requirements.dev.txt"
   "--find-links http://some.archives.com/archives"]
   ["ok-dep=~0.1.2"
    "yet_another_ok_package>=1.2.7,<2"]
   ["ok-dep=~0.1.2"]])


(deftest test-line->dep
  (testing "Spliting line to dependency vector"
    (doseq [[line expected-vector description] dependencies]
      (testing description
        (is (= expected-vector (file/line->dep line)))))))


(deftest test-filtered-lines-no-extra-filter
  (testing "Filtering strings with default filters"
    (let [[input-lines expected-lines] requirements-lines]
      (is (= expected-lines (file/filtered-lines input-lines))))))


(deftest test-filtered-lines-with-extra-filter
  (testing "Extra filters to exclude yet_another* packages"
    (let [[input-lines _ expected-lines] requirements-lines]
      (is (= expected-lines
             (file/filtered-lines input-lines "(yet_another).*"))))))
