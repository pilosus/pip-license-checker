(ns pip-license-checker.version-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [pip-license-checker.version :as v]))

(defn enum
  "Enumerate sequence ([idx1 elem1] [id2 elem2] ...)"
  [s]
  (map vector (range) s))

(def params-version
  "This list must be in the correct sorting order"
  [;; Implicit epoch of 0
   "1.0.dev456",
   "1.0a1",
   "1.0a2.dev456",
   "1.0a12.dev456",
   "1.0a12",
   "1.0b1.dev456",
   "1.0b2",
   "1.0b2.post345.dev456",
   "1.0b2.post345",
   "1.0b2-346",
   "1.0c1.dev456",
   "1.0c1",
   "1.0rc2",
   "1.0c3",
   "1.0",
   "1.0.post456.dev34",
   "1.0.post456",
   "1.1.dev1",
   "1.2+123abc",
   "1.2+123abc456",
   "1.2+abc",
   "1.2+abc123",
   "1.2+abc123def",
   "1.2+1234.abc",
   "1.2+123456",
   "1.2.r32+123456",
   "1.2.rev33+123456",
   ;; Explicit epoch of 1
   "1!1.0.dev456",
   "1!1.0a1",
   "1!1.0a2.dev456",
   "1!1.0a12.dev456",
   "1!1.0a12",
   "1!1.0b1.dev456",
   "1!1.0b2",
   "1!1.0b2.post345.dev456",
   "1!1.0b2.post345",
   "1!1.0b2-346",
   "1!1.0c1.dev456",
   "1!1.0c1",
   "1!1.0rc2",
   "1!1.0c3",
   "1!1.0",
   "1!1.0.post456.dev34",
   "1!1.0.post456",
   "1!1.1.dev1",
   "1!1.2+123abc",
   "1!1.2+123abc456",
   "1!1.2+abc",
   "1!1.2+abc123",
   "1!1.2+abc123def",
   "1!1.2+1234.abc",
   "1!1.2+123456",
   "1!1.2.r32+123456",
   "1!1.2.rev33+123456"])

(def params-parse-version
  [["1.0.dev456"
    {:orig "1.0.dev456"
     :epoch 0
     :release [1 0]
     :pre nil
     :post nil
     :dev ["dev" 456]
     :local nil}
    "Release and dev version"]
   ["1!1.0b2.post345.dev456"
    {:orig "1!1.0b2.post345.dev456"
     :epoch 1
     :release [1 0]
     :pre ["b" 2]
     :post ["post" 345]
     :dev ["dev" 456]
     :local nil}
    "Release, pre, post and dev version"]])

(deftest test-parse-version
  (testing "Version parsing"
    (doseq [[version expected description] params-parse-version]
      (testing description
        (is (= expected (v/parse-version version)))))))
