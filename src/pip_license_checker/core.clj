(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [clojure.walk :as walk]))


;; Const

(def pypi-base-url "https://pypi.org/pypi/")
(def pypi-license-classifier-regex #"License :: .*")
(def pypi-classifier-split-regex #" :: ")
(def license-error-name "Error")


;; Logic

(defn http-get-or-nil
  "Return response of HTTP GET request or nil in case of exception"
  [url]
  (try
    (http/get url)
    (catch Exception e nil)))


(defn get-package-response
  "Return response of a request to PyPI package page of given name and version"
  [name version]
  (let [url (str/join "/" [pypi-base-url name version "json"])]
    (http-get-or-nil url)))


(defn parse-package-response-body
  "Parse JSON string to a hashmap"
  [data]
  (try
    (walk/keywordize-keys (json/parse-string (:body data)))
    (catch Exception e nil)))


(defn find-license-in-classifiers
  "Return license classifier or nil"
  [body]
  (let [classifiers (:classifiers (:info body))]
    (if (some? classifiers)
      (let [license-classifier 
            (first 
             (filter 
              (fn [classifier] (re-matches pypi-license-classifier-regex classifier)) 
              classifiers))]
        (if (some? license-classifier)
          (last (str/split license-classifier pypi-classifier-split-regex))
          nil))
      nil)))


(defn find-license
  "Find license in PyPI package response body"
  [body]
  (let [license-field (:license (:info body))]
    (if
        (some? license-field)
        license-field
        (find-license-in-classifiers body))))


(defn get-license
  "Return string with PyPI package license"
  [name version]
  (let [data (get-package-response name version)]
    (if (some? data)
      (find-license (parse-package-response-body data))
      license-error-name)))


;; Entry point

(defn get-description
  "Return multiline description"
  [& strings]
  (str/join "\n" strings))


(def usage
  "Usage message"
  (get-description
   "Usage:"
   "pip_license_checker name version"
   "  name: name of existing PyPI package"
   "  version: version of the package"))


(defn -main
  "App entry point"
  [& args]
  (if (= (count args) 2)
    (println (get-license (first args) (second args)))
    (println usage)))
