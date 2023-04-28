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

(ns pip-license-checker.logging-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.logging :as l]))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

(def params-get-error-message
  [[(try (ex-info "test" {:status 429 :reason-phrase "Rate limits exceeded"})
         (catch Exception e e))
    "429 Rate limits exceeded"
    "exception 1"]
   [(try (/ 1 0) (catch Exception e e))
    "ArithmeticException Divide by zero"
    "exception 2"]])

(deftest test-get-error-message
  (testing "Test getting info from exception"
    (doseq [[exception expected description] params-get-error-message]
      (testing description
        (is (= expected (l/get-error-message exception)))))))

(def params-get-log-level-number
  [[{} 100 "silent implicitly"]
   [{:verbose 0} 100 "silent explicitly"]
   [{:verbose 1} 30 "error"]
   [{:verbose 2} 20 "info"]
   [{:verbose 3} 10 "debug"]
   [{:verbose 123} 10 "debug is the maximum verbosity"]])

(deftest test-get-log-level-number
  (testing "Test getting log level number"
    (doseq [[options expected description] params-get-log-level-number]
      (testing description
        (is (= expected (l/get-log-level-number options)))))))

(def logs
  {:checker {:level :error :name "Checker" :message "Something went terribly wrong"}
   :yanked {:level :info :name "PyPI::version" :message "Yanked"}
   :env {:level :info :name "PyPI::version" :message "Doesn't satisfy system env"}
   :fallback {:level :info :name "GitHub::license" :message "Fallback to GitHub"}
   :tls {:level :debug :name "clj-http" :message "TLS handshake"}})

(def log-list
  [(:checker logs)
   (:yanked logs)
   (:env logs)
   (:fallback logs)
   (:tls logs)])

(def params-format-logs
  [[{} log-list "" "silent implicitly"]
   [{:verbose 0} log-list "" "silent explicitly"]
   [{:verbose 1}
    log-list
    "Error: Checker Something went terribly wrong"
    "error level"]
   [{:verbose 2}
    log-list
    "Error: Checker Something went terribly wrong\nInfo: GitHub::license Fallback to GitHub\nInfo: PyPI::version Doesn't satisfy system env\nInfo: PyPI::version Yanked"
    "info level"]
   [{:verbose 3}
    log-list
    "Error: Checker Something went terribly wrong\nInfo: GitHub::license Fallback to GitHub\nInfo: PyPI::version Doesn't satisfy system env\nInfo: PyPI::version Yanked\nDebug: clj-http TLS handshake"
    "debug level"]
   [{:verbose 35}
    log-list
    "Error: Checker Something went terribly wrong\nInfo: GitHub::license Fallback to GitHub\nInfo: PyPI::version Doesn't satisfy system env\nInfo: PyPI::version Yanked\nDebug: clj-http TLS handshake"
    "super verbosity is still a debug level"]])

(deftest test-format-logs
  (testing "Test logs formatting"
    (doseq [[options logs expected description] params-format-logs]
      (testing description
        (is (= expected (l/format-logs logs options)))))))
