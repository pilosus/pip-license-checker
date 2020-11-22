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
(def ignore-case-regex-modifier #"(?i)")
(def license-error-name "Error")
(def copyleft-license "Copyleft")
(def not-copyleft-license "No Copyleft")
(def error-copyleft-license "???")
(def copyleft-licenses
  "Free software licenses (Copyleft)"
  ;; https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licences
  [
   #"Affero"
   #"EUPL"
   #"European Union Public Licence"
   #"FDL"
   #"GNU Affero General Public License"
   #"GNU Free Documentation License"
   #"GNU General Public License"
   #"GPL"
   #"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"GNU"
   #"IBM Public License"
   #"LGPL"
   #"MPL"
   #"Mozilla Public License"
   #"OSL"
   #"Open Software License"])


;; Logic

(defn concat-re-patterns-
  [patterns]
  (re-pattern (apply str (interpose "|" (map #(str "(" % ")") patterns)))))


(defn combine-re-patterns
  "Concatenate sequence of regex into a single one with optional regexp modifier"
  ([patterns]
   (concat-re-patterns- patterns))
  ([modifier patterns]
   (re-pattern (str modifier (concat-re-patterns- patterns)))))


(defn get-copyleft-verdict
  "Return string with a verdict if license name matches one of the copyleft licenses"
  [name]
  (let [pattern (combine-re-patterns ignore-case-regex-modifier copyleft-licenses)
        matches (some some? (re-find pattern name))]
    (cond
      (= name license-error-name) error-copyleft-license
      (true? matches) copyleft-license
      :else not-copyleft-license)))


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
  (let [license-field-data (:license (:info body))
        classifiers-field-data (find-license-in-classifiers body)]
    (if
        (some? classifiers-field-data)
        classifiers-field-data
        license-field-data)))


(defn get-license
  "Return string with PyPI package license"
  [name version]
  (let [data (get-package-response name version)]
    (if (some? data)
      (find-license (parse-package-response-body data))
      license-error-name)))


(defn get-license-name-with-verdict
  [name version]
  (let [license-name (get-license name version)
        license-verdict (get-copyleft-verdict license-name)]
    (format "%s:%-30s %-30s %s" name version license-name license-verdict)))


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
    (println (get-license-name-with-verdict (first args) (second args)))
    (println usage)))
