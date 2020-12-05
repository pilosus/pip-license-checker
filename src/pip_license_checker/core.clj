(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.io :as io]
   [pip-license-checker.file :as file]))


;; Const

(def http-settings
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})


(def pypi-latest-version "latest")
(def pypi-base-url "https://pypi.org/pypi")


(def requirement-args-regex #"^(?:--requirement|-r)")
(def pypi-license-classifier-regex #"License :: .*")
(def pypi-classifier-split-regex #" :: ")
(def ignore-case-regex-modifier #"(?i)")

(def license-error-name "Error")

(def copyleft-license-type "Copyleft")
(def permissive-license-type "Permissive")
(def other-license-type "Other")
(def error-license-type "???")

(def copyleft-licenses
  "Free software licenses (Copyleft)"
  ;; https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licences
  [
   #"^Affero"
   #"^EUPL"
   #"European Union Public Licence"
   #"^FDL"
   #"GNU Affero General Public License"
   #"GNU Free Documentation License"
   #"GNU General Public License"
   #"^GPL"
   #"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"^GNU"
   #"IBM Public License"
   #"^LGPL"
   #"^MPL"
   #"Mozilla Public License"
   #"^OSL"
   #"Open Software License"])


;;
;; Logic
;;

;; Copyleft verdict

(defn concat-re-patterns
  [patterns]
  (re-pattern (apply str (interpose "|" (map #(str "(" % ")") patterns)))))


(defn combine-re-patterns
  "Concatenate sequence of regex into a single one with optional regexp modifier"
  ([patterns]
   (concat-re-patterns patterns))
  ([modifier patterns]
   (re-pattern (str modifier (concat-re-patterns patterns)))))


(defn get-copyleft-verdict
  "Return string with a verdict if license name matches one of the copyleft licenses"
  [name]
  (let [pattern (combine-re-patterns ignore-case-regex-modifier copyleft-licenses)
        matches (some some? (re-find pattern name))]
    (cond
      (= name license-error-name) error-license-type
      (true? matches) copyleft-license-type
      :else other-license-type)))


;; API requests

(defn http-get-or-nil
  "Return response of HTTP GET request or nil in case of exception"
  [url]
  (try
    (http/get url http-settings)
    (catch Exception e nil)))


(defn get-package-response
  "Return response of a request to PyPI package page of given name and version"
  [name version]
  (let [url
        (if (= version pypi-latest-version)
          (str/join "/" [pypi-base-url name "json"])
          (str/join "/" [pypi-base-url name version "json"]))]
    (http-get-or-nil url)))


;; JSON parsing

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
      (let [license-name (find-license (parse-package-response-body data))]
        (if (some? license-name)
          license-name
          license-error-name))
      license-error-name)))


(defn get-license-name-with-verdict
  ([name]
   (get-license-name-with-verdict name pypi-latest-version))
  ([name version]
   (let [license-name (get-license name version)
        license-verdict (get-copyleft-verdict license-name)
         package-name (if (some? version)
                        (str name ":" version)
                        name)]
     (format "%-50s %-50s %-50s" package-name license-name license-verdict))))


;; Entry point

(defn get-description
  "Return multiline description"
  [& strings]
  (str/join "\n" strings))


(def usage
  "Usage message"
  (get-description
   "Usage:"
   "pip_license_checker [name] [version]"
   "pip_license_checker [-r|--requirement] [path]"
   "  name: name of existing PyPI package"
   "  version: version of the package"
   "  -r, --requirement: option flag to scan requirements.txt file"
   "  path: path to a requirements text file"))


(defn multiple-args-start
  "Helper for multiple argument main start"
  [first-arg second-arg]
  (if (re-matches requirement-args-regex first-arg)
    (file/print-file second-arg get-license-name-with-verdict)
    (println (get-license-name-with-verdict first-arg second-arg))))


(defn -main
  "App entry point"
  [& args]
  (let [[first-arg second-arg] args
        num-of-args (count args)]
    (cond
      (= num-of-args 1) (println (get-license-name-with-verdict first-arg))
      (= num-of-args 2) (multiple-args-start first-arg second-arg)
      :else (println usage))))
