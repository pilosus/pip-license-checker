;; Copyright © 2020-2022 Vitaly Samigullin
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

(ns pip-license-checker.exception-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.exception :as ex]))

(def params-get-ex-info
  [["module 1"
    (ex-info "test" {:cause "too bad"})
    "[module 1] ExceptionInfo: test"
    "exception 1"]
   ["module 2"
    (try (/ 1 0) (catch Exception e e))
    "[module 2] ArithmeticException: Divide by zero"
    "exception 2"]])

(deftest test-params-get-ex-info
  (testing "Test getting info from exception"
    (doseq [[logger exception expected description] params-get-ex-info]
      (testing description
        (is (= expected (ex/get-ex-info logger exception)))))))


;; join-ex-info


(def params-join-ex-info
  [[[]
    nil
    "Empty vector"]
   [[nil nil nil]
    nil
    "Nils"]
   [nil
    nil
    "Nil args"]
   [["Not found"]
    "Not found"
    "Single error message"]
   [["Not found" "Rate limits exceeded" "Connection timeout"]
    "Not found; Rate limits exceeded; Connection timeout"
    "Multiple error messages"]])

(deftest test-join-ex-info
  (testing "Test joining multiple exceptions into a message"
    (doseq [[args expected description] params-join-ex-info]
      (testing description
        (is (= expected (apply ex/join-ex-info args)))))))
