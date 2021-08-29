(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   ;;[clojure.spec.test.alpha :refer [instrument]]
   [clojure.set :refer [intersection]]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [pip-license-checker.external :as external]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.spec :as sp]))

(def formatter-license "%-35s %-55s %-30s")
(def formatter-totals "%-35s %-55s")

(defn exit
  "Exit from the app with exit status"
  ([status]
   (System/exit status))
  ([status msg]
   (println msg)
   (exit status)))

(defn print-license-header
  []
  (println
   (format formatter-license "Requirement" "License Name" "License Type")))

(defn format-license
  "Print requirement and its license"
  [license-data]
  (let [{:keys [requirement license]} license-data
        {req-name :name req-version :version} requirement
        package
        (if req-version (str req-name ":" req-version) req-name)
        {lic-name :name lic-type :type} license]
    (format formatter-license package lic-name lic-type)))

(s/fdef get-license-type-totals
  :args (s/coll-of ::sp/requirement-response-license)
  :ret ::sp/license-type-totals)

(defn get-license-type-totals
  "Return a frequency map of license types as keys and license types as values"
  [licenses]
  (let [freqs (frequencies (map #(:type (:license %)) licenses))
        ordered-freqs (into (sorted-map) freqs)]
    ordered-freqs))

(defn format-total
  "Print lincese type  totals line"
  [license-type freq]
  (format formatter-totals license-type freq))

(defn print-totals-header
  []
  (println (format formatter-totals "License Type" "Found")))

(s/fdef process-requirements
  :args (s/cat
         :requirements ::sp/requirements-cli-arg
         :packages ::sp/packages-cli-arg
         :options ::sp/options-cli-arg))

(defn process-requirements
  "Print parsed requirements pretty"
  [packages requirements external options]
  (let [fail-opt (:fail options)
        with-fail (seq fail-opt)
        with-totals-opt (:with-totals options)
        totals-only-opt (:totals-only options)
        show-totals (or with-totals-opt totals-only-opt)
        table-headers (:table-headers options)
        parsed-external-licenses (external/get-parsed-requiements external options)
        parsed-pypi-licenses (pypi/get-parsed-requiements packages requirements options)
        parsed-licenses (concat parsed-pypi-licenses parsed-external-licenses)
        licenses (filters/filter-parsed-requirements parsed-licenses options)
        totals
        (if (or show-totals with-fail)
          (get-license-type-totals licenses)
          nil)
        totals-keys (into (sorted-set) (keys totals))
        fail-types-found (intersection totals-keys fail-opt)
        fail? (seq fail-types-found)]

    (when (not totals-only-opt)
      (when table-headers
        (print-license-header))

      (doseq [line licenses]
        (println (format-license line))))

    (when with-totals-opt
      (println))

    (when show-totals
      (when table-headers
        (print-totals-header))

      (doseq [[license-type freq] totals]
        (println (format-total license-type freq))))

    ;; shutdown a thread pool used by pmap to allow JVM shutdown
    (shutdown-agents)

    (when fail?
      (exit 1))))

(defn usage [options-summary]
  (->> ["pip-license-checker - license compliance tool to identify dependencies license names and types."
        ""
        "Usage:"
        "pip-license-checker [options]... [package]..."
        ""
        "Description:"
        "  package\tList of Python package names in format `name[specifier][version]`"
        ""
        options-summary
        ""
        "Examples:"
        "pip-license-checker django"
        "pip-license-checker aiohttp==3.7.2 piny==0.6.0 django"
        "pip-license-checker --pre 'aiohttp<4'"
        "pip-license-checker --with-totals --table-headers --requirements resources/requirements.txt"
        "pip-license-checker --totals-only -r file1.txt -r file2.txt -r file3.txt"
        "pip-license-checker -r resources/requirements.txt django aiohttp==3.7.1 --exclude 'aio.*'"
        "pip-license-checker -x resources/external.csv --exclude-license '(?i).*(?:mit|bsd).*'"
        "pip-license-checker -x resources/external.csv --external-options '{:skip-header false}'"
        "pip-license-checker -x resources/external.cocoapods --external-format cocoapods'"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing command arguments:\n"
       (str/join \newline errors)))

(def cli-options
  [["-r" "--requirements REQUIREMENTS_FILE" "Python pip requirement file name"
    :multi true
    :default []
    :update-fn conj
    :validate [file/exists? "Requirements file does not exist"]]
   ["-x" "--external FILE_NAME" "File containing package names and license names"
    :multi true
    :default []
    :update-fn conj
    :validate [file/exists? "File does not exist"]]
   [nil "--external-format FILE_FORMAT" "External file format: csv, cocoapods, gradle"
    :default external/format-csv
    :validate [external/is-format-valid? external/invalid-format]]
   [nil "--external-options OPTS_EDN_STRING" "String of options map in EDN format"
    :default external/default-options
    :parse-fn external/opts-str->map]
   ["-f" "--fail LICENSE_TYPE" "Return non-zero exit code if license type is found"
    :default (sorted-set)
    :multi true
    :update-fn conj
    :validate [license/is-type-valid? license/invalid-type]]
   ["-e" "--exclude REGEX" "PCRE to exclude packages with matching names" :parse-fn #(re-pattern %)]
   [nil "--exclude-license REGEX" "PCRE to exclude packages with matching license names" :parse-fn #(re-pattern %)]
   [nil "--[no-]pre" "Include pre-release and development versions. By default, use only stable versions" :default false]
   [nil "--[no-]with-totals" "Print totals for license types" :default false]
   [nil "--[no-]totals-only" "Print only totals for license types" :default false]
   [nil "--[no-]table-headers" "Print table headers" :default false]
   [nil "--[no-]fails-only" "Print only packages of license types specified with --fail flags" :default false]
   ["-h" "--help" "Print this help message"]])

(s/fdef extend-fail-opt
  :args (s/? ::sp/options-fail)
  :ret (s/? ::sp/options-fail))

(defn extend-fail-opt
  "Try to substitute common fail option in set with specific parts it consist of"
  [fail-opts]
  (if (contains? fail-opts license/type-copyleft-all)
    (clojure.set/difference
     (clojure.set/union fail-opts license/types-copyleft)
     #{license/type-copyleft-all})
    fail-opts))

(s/fdef post-process-options
  :args (s/? ::sp/options-cli-arg)
  :ret (s/? ::sp/options-cli-arg))

(defn post-process-options
  "Update option map"
  [options]
  (let [opts' (dissoc options :requirements :external)
        fail-opt (:fail opts')
        fail-opt-exteded (extend-fail-opt fail-opt)
        updated-opts (assoc opts' :fail fail-opt-exteded)]
    updated-opts))

(s/fdef validate-args
  :args (s/cat :args (s/coll-of string?))
  :ret (s/cat
        :exit-message (s/? string?)
        :ok? (s/? boolean?)
        :requirements (s/? ::sp/requirements-cli-arg)
        :packages (s/? ::sp/packages-cli-arg)
        :options (s/? ::sp/options-cli-arg)))

(defn validate-args
  "Parse and validate CLI arguments for entrypoint"
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors {:exit-message (error-msg errors)}
      (or
       (> (count arguments) 0)
       (> (count (:requirements options)) 0)
       (> (count (:external options)) 0))
      {:requirements (:requirements options)
       :external (:external options)
       :packages arguments
       :options (post-process-options options)}
      :else
      {:exit-message (usage summary)})))

(defn -main
  "App entry point"
  [& args]
  (let [{:keys [packages requirements external options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (process-requirements packages requirements external options))))


;;
;; Instrumented functions - uncomment only while testing
;;

;; (instrument `process-requirements)
;; (instrument `validate-args)
