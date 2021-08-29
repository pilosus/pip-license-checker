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

(ns pip-license-checker.github-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [pip-license-checker.github :as github]))

(def params-get-license-name
  [[["" "owner" "repo"]
    (constantly {:body "{\"license\": {\"name\": \"MIT License\"}}"})
    "MIT License"
    "Ok"]
   [["" "owner" "repo"]
    (constantly {:body "{\"errors\": {\"message\": \"No License Found\"}}"})
    nil
    "Fallback"]
   [["" "owner" "repo"]
    (fn [& _] (throw (Exception. "Boom!")))
    nil
    "Expcetion"]])

(deftest test-get-license-name
  (testing "Get license name from GitHub API"
    (doseq [[path-parts body-mock expected description] params-get-license-name]
      (testing description
        (with-redefs [http/get body-mock]
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
    "Ok GitHub URL"]
   [nil
    "{\"license\": {\"name\": \"MIT License\"}}"
    nil
    "nil URL"]])

(deftest test-homepage->license-name
  (testing "Get license name from project url if it is GitHub"
    (doseq [[url response expected description] params-homepage->license-name]
      (testing description
        (with-redefs [http/get (constantly {:body response})]
          (is (= expected (github/homepage->license-name url))))))))
