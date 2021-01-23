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
