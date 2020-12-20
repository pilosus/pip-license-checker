(ns pip-license-checker.github-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [pip-license-checker.github :as github]))

(def params-get-license-name
  [[["" "owner" "repo"]
    "{\"license\": {\"name\": \"MIT License\"}}"
    "MIT License"
    "Ok"]
   [["" "owner" "repo"]
    "{\"errors\": {\"message\": \"No License Found\"}}"
    nil
    "Fallback"]])

(deftest test-get-license-name
  (testing "Get license name from GitHub API"
    (doseq [[path-parts response expected description] params-get-license-name]
      (testing description
        (with-redefs [http/get (constantly {:body response})]
          (is (= expected (github/get-license-name path-parts))))))))

(def params-homepage->license-name
  [["http://example.com"
    "{\"license\": {\"name\": \"MIT License\"}}"
    nil
    "Not a GitHub URL"]
   ["https://github.com/pilosus"
    "{\"license\": {\"name\": \"MIT License\"}}"
    nil
    "Malformed GitHub URL"]
   ["https://github.com/pilosus/piny/"
    "{\"license\": {\"name\": \"MIT License\"}}"
    "MIT License"
    "Ok GitHub URL"]])

(deftest test-homepage->license-name
  (testing "Get license name from project url if it is GitHub"
    (doseq [[url response expected description] params-homepage->license-name]
      (testing description
        (with-redefs [http/get (constantly {:body response})]
          (is (= expected (github/homepage->license-name url))))))))
