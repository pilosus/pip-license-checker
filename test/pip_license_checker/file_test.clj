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
   ["SomeProject == 1.3.4 @ file:///somewhere/", ["SomeProject", "1.3.4"] "Direct reference"]
   ["https://example.com/package_3.0.3.dev1820+49a8884-cp34-none-win_amd64.whl" nil "File path"]
   ["#aiohttp==3.7.2" nil "Comment"]
   ["-r requirements.dev.txt" nil "Option starting with -"]
   ["--find-links http://some.archives.com/archives" nil "Option starting with --"]])



(deftest test-line->dep
  (testing "Spliting line to dependency vector"
    (doseq [[line expected-vector description] dependencies]
      (testing description
        (is (= expected-vector (file/line->dep line)))))))
