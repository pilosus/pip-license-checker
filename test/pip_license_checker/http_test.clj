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

(ns pip-license-checker.http-test
  (:require
   [clj-http.client :as clj-http]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :refer [deftest is testing]]
   [indole.core :refer [make-rate-limiter]]
   [pip-license-checker.http :as http]))

;; set up assertions for spec validation
(s/check-asserts true)

;; instrument all functions to test functions :args
(stest/instrument)

;; check all functions :ret and :fn
(stest/check)

(def hit-counter (atom 0))

(def params-request-get
  [[10 20 false "Rate limit miss"]
   [10 9 true "Rate limit hit"]])

(defn delay-mock
  []
  (swap! hit-counter inc)
  (Thread/sleep 100))

(deftest test-request-get
  (testing "GET request with rate limiting"
    (doseq [[requests-todo requests-limit expected description] params-request-get]
      (testing description
        (with-redefs
         [clj-http/get (constantly true)
          http/make-delay delay-mock]
          (let [rate-limiter (make-rate-limiter 60000 requests-limit)
                _ (dotimes [_ requests-todo]
                    (http/request-get "https://example.com/" {} rate-limiter))
                limit-hit? (> @hit-counter 0)]
            (is (= expected limit-hit?))))))))
