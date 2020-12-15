(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [clojure.java.io :as io]
   [clojure.tools.cli :refer [parse-opts]]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.parsers :as parsers]
))


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
   #"IBM Public License"
   #"^MPL"
   #"Mozilla Public License"
   #"^OSL"
   #"Open Software License"])

(def permissive-licenses
  [
   #"CeCILL-B Free Software License Agreement"
   #"^CeCILL-B"
   #"License :: CeCILL-C Free Software License Agreement"
   #"^CeCILL-C"
   #"CEA CNRS Inria Logiciel Libre License"
   #"^CeCILL-2.1"  ;; http://cecill.info/index.en.html
   #"Academic Free License"
   #"^AFL"
   #"Apache Software License"
   #"^Apache"
   #"BSD License"
   #"^BSD"
   #"Historical Permission Notice and Disclaimer"
   #"^HPND"
   #"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"^LGPL"
   #"MIT License"
   #"^MIT"
   #"ISC License"
   #"^ISCL"
   #"Python Software Foundation License"
   #"Python License"
   #"Unlicense"
   #"Universal Permissive License"
   #"UPL"
   #"W3C License"
   #"W3C"
   #"zlib/libpng License"
   #"zlib/libpng"
   #"Public Domain"
   ])


;;
;; Logic
;;

;; Copyleft verdict

(defn concat-re-patterns
  [patterns]
  (re-pattern (apply str (interpose "|" (map #(str "(?:" % ")") patterns)))))


(defn combine-re-patterns
  "Concatenate sequence of regex into a single one with optional regexp modifier"
  ([patterns]
   (concat-re-patterns patterns))
  ([modifier patterns]
   (re-pattern (str modifier (concat-re-patterns patterns)))))


(defn find-license-type
  "Return string with a verdict of license type"
  [name]
  (let
      [copyleft-pattern
       (combine-re-patterns ignore-case-regex-modifier copyleft-licenses)
       copyleft-matches (some? (re-find copyleft-pattern name))
       permissive-pattern
       (combine-re-patterns ignore-case-regex-modifier permissive-licenses)
       permissive-matches (some? (re-find permissive-pattern name))]
      (cond
        (= name license-error-name) error-license-type
        (true? copyleft-matches) copyleft-license-type
        (true? permissive-matches) permissive-license-type
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
        license-verdict (find-license-type license-name)
         package-name (if (some? version)
                        (str name ":" version)
                        name)]
     (format "%-35s %-55s %-30s" package-name license-name license-verdict))))

;;
;; [+] Get all packages from CLI args into one vector ->
;; [+] Apply internal filters ->
;; [+] Apply user-defined filters ->
;; [+] Parse packages to [name version] ->
;; [] Fetch license for each package ->
;; [] Format output (? possibly print total stats)
;;


(defn get-all-requirements
  "Get a sequence of all requirements"
  [packages requirements]
  (let [file-packages (file/get-requirement-lines requirements)]
    (concat packages file-packages)))



;;
;; Entry point
;;


(defn process-requirements
  "Apply filters and get verdicts for all requirements"
  [packages requirements options]
  (let [exclude-pattern (:exclude options)]
    (->> (get-all-requirements packages requirements)
         (filters/remove-requirements-internal-rules)
         (filters/remove-requirements-user-rules exclude-pattern)
         (map filters/sanitize-requirement)
         (map filters/requirement->map)
)))



(defn usage [options-summary]
  (->> ["pip-license-checker - check Python PyPI package license"
        ""
        "Usage:"
        "pip-license-checker [options]... [package]..."
        ""
        "Description:"
        "  package                      List of package names in format `name[==version]`"
        ""
        options-summary
        ""
        "Examples:"
        "pip-license-checker django"
        "pip-license-checker aiohttp:3.7.2 piny:0.6.0 django"
        "pip-license-checker -r resources/requirements.txt"
        "pip-license-checker -r file1.txt -r file2.txt -r file3.txt"
        "pip-license-checker --requirements resources/requirements.txt --exclude 'aio.*'"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing:\n"
       (str/join \newline errors)))

(def cli-options
  [
   ;; FIXME update with :multi true update-fn: conj once clojure.tools.cli 1.0.195 (?) released
   ["-r" "--requirements NAME" "Requirement file name to read"
    :default []
    :assoc-fn #(update %1 %2 conj %3)
    :validate [#(.exists (io/file %)) "Requirement file does not exist"]]
   ["-e" "--exclude REGEX" "PCRE to exclude matching packages. Used only if [package]... or requirement files specified"
    :parse-fn #(re-pattern %)]
   ["-h" "--help"]])

(defn validate-args
  "Parse and validate CLI arguments for entrypoint"
  [args]
  (let [{:keys [options arguments errors summary] :as parsed-args} (parse-opts args cli-options)]
    (println parsed-args)  ;; FIXME remove after debugging
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (or
       (> (count arguments) 0)
       (> (count (:requirements options)) 0))
      {:requirements (:requirements options)
       :packages arguments
       :options (dissoc options :requirements)}
      :else
      {:exit-message (usage summary)})))

(defn exit
  "Exit from the app with exit status"
  [status msg]
  (println msg)
  (System/exit status))

(defn -main
  "App entry point"
  [& args]
  (let [{:keys [packages requirements options exit-message ok?] :as parsed-args} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (println (process-requirements packages requirements options)))))
