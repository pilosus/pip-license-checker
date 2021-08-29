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

(ns pip-license-checker.spec-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.spec :as sp]))

(def ->int
  (sp/with-conformer [val] (Integer/parseInt val)))

(s/def ::->int ->int)

(def ->requirements
  (sp/with-conformer [val] (str/split val #",")))

(s/def ::->requirements ->requirements)

(def params-test-with-conformer-macro
  [["123" ::->int 123 "Normal integer parsing"]
   ["abc" ::->int ::s/invalid  "Do not throw execption for int parsing error"]
   ["aiohttp,django,celery"
    ::->requirements
    ["aiohttp" "django" "celery"]
    "Normal requirements string parsing"]
   [123 ::->requirements ::s/invalid "Fail gracefully on requirements string parsing"]])

(deftest test-with-conformer-macro
  (testing "Conforming data to specs with macro"
    (doseq [[input conformer expected description] params-test-with-conformer-macro]
      (testing description
        (is (= expected (s/conform conformer input)))))))

(def params-regex?
  [[#"^(aio|test[-_]).*" true "Regex"]
   ["string" false "Not regex"]])

(deftest test-regex?
  (testing "Is data regular expression?"
    (doseq [[input expected description] params-regex?]
      (testing description
        (is (= expected (sp/regex? input)))))))
