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

(ns pip-license-checker.github-test
  (:require
   [clj-http.client :as http]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [indole.core :refer [make-rate-limiter]]
   [pip-license-checker.github :as g]
   [pip-license-checker.spec :as sp]
))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)


(def rate-limits (make-rate-limiter 1000 100))

(def params-api-get-license
  [[["" "owner" "repo"]
    (constantly {:body "{\"license\": {\"name\": \"MIT License\"}}"})
    (s/assert ::sp/license {:name "MIT License" :type nil :logs nil})
    "Ok"]
   [["" "owner" "repo"]
    (constantly {:body "{\"errors\": {\"message\": \"No License Found\"}}"})
    (s/assert ::sp/license {:name nil :type nil :logs nil})
    "Fallback"]
   [["" "owner" "repo"]
    (fn [& _] (throw (ex-info "Boom!" {:status 404 :reason-phrase "Page not found"})))
    (s/assert
     ::sp/license
     {:name nil
      :type nil
      :logs [{:level :error
              :name "GitHub::license"
              :message "404 Page not found"}]})
    "Exception"]])

(deftest test-api-get-license
  (testing "Get license name from GitHub API"
    (doseq [[path-parts body-mock expected description] params-api-get-license]
      (testing description
        (with-redefs [http/get body-mock]
          (is (= expected (g/api-get-license path-parts {} rate-limits))))))))

(def params-homepage->license
  [["http://example.com"
    "{\"license\": {\"name\": \"MIT License\"}}"
    (s/assert ::sp/license {:name nil :type nil :logs [g/meta-not-found]})
    "Not a GitHub URL"]
   ["https://github.com/pilosus"
    "{\"license\": {\"name\": \"MIT License\"}}"
    (s/assert ::sp/license {:name nil :type nil :logs [g/meta-not-found]})
    "Malformed GitHub URL"]
   ["https://github.com/pilosus/piny/"
    "{\"license\": {\"name\": \"MIT License\"}}"
    (s/assert ::sp/license {:name "MIT License" :type nil :logs nil})
    "Ok GitHub URL"]
   [nil
    "{\"license\": {\"name\": \"MIT License\"}}"
    (s/assert ::sp/license {:name nil :type nil :logs [g/meta-not-found]})
    "nil URL"]])

(deftest test-homepage->license
  (testing "Get license name from project url if it is GitHub"
    (doseq [[url response expected description] params-homepage->license]
      (testing description
        (with-redefs [http/get (constantly {:body response})]
          (is (= expected (g/homepage->license url {} rate-limits))))))))

(def default-headers {:headers g/header-github-api-version})

(def params-get-headers
  [[{} default-headers "No options"]
   [{:some 1 :options 2} default-headers "No github token option"]
   [{:github-token nil} default-headers "Token provided but invalid"]
   [{:github-token "hello"}
    {:headers (merge {"Authorization" "Bearer hello"}
                     g/header-github-api-version)}
    "Valid token provided"]])

(deftest test-get-headers
  (testing "Test HTTP request headers generation"
    (doseq [[options expected description] params-get-headers]
      (testing description
        (is (= expected (g/get-headers options)))))))
