(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.pypi :as pypi]))

(defn format-license
  "Print requirement and its license"
  [license-data]
  (let [{:keys [requirement license]} license-data
        {req-name :name req-version :version} requirement
        package
        (str req-name ":" req-version)
        {lic-name :name lic-desc :desc} license]
    (format "%-35s %-55s %-30s" package lic-name lic-desc)))

(defn get-all-requirements
  "Get a sequence of all requirements"
  [packages requirements]
  (let [file-packages (file/get-requirement-lines requirements)]
    (concat packages file-packages)))

(defn get-parsed-requiements
  "Apply filters and get verdicts for all requirements"
  [packages requirements options]
  (let [exclude-pattern (:exclude options)
        licenses (->> (get-all-requirements packages requirements)
                      (filters/remove-requirements-internal-rules)
                      (filters/remove-requirements-user-rules exclude-pattern)
                      (map filters/sanitize-requirement)
                      (map filters/requirement->map)
                      (map pypi/requirement->license))]
    licenses))

(defn process-requirements
  "Print parsed requirements pretty"
  [packages requirements options]
  (let [licenses (get-parsed-requiements packages requirements options)]
    (doseq [line licenses]
      (println (format-license line)))))

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
        "pip-license-checker aiohttp==3.7.2 piny==0.6.0 django"
        "pip-license-checker --requirements resources/requirements.txt"
        "pip-license-checker -r file1.txt -r file2.txt -r file3.txt"
        "pip-license-checker -r resources/requirements.txt django aiohttp==3.7.1 --exclude 'aio.*'"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing command arguments:\n"
       (str/join \newline errors)))

(def cli-options
  [;; FIXME update with :multi true update-fn: conj once clojure.tools.cli 1.0.195 (?) released
   ["-r" "--requirements NAME" "Requirement file name to read"
    :default []
    :assoc-fn #(update %1 %2 conj %3)
    :validate [file/exists? "Requirement file does not exist"]]
   ["-e" "--exclude REGEX" "PCRE to exclude matching packages. Used only if [package]... or requirement files specified"
    :parse-fn #(re-pattern %)]
   ["-h" "--help"]])

(defn validate-args
  "Parse and validate CLI arguments for entrypoint"
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
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
  (let [{:keys [packages requirements options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (process-requirements packages requirements options))))
