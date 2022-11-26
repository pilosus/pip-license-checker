;; Copyright © 2020-2022 Vitaly Samigullin
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

(ns pip-license-checker.core
  "License fetcher for Python PyPI packages"
  (:gen-class)
  (:require
   [clojure.set :refer [intersection]]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [pip-license-checker.external :as external]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.report :as report]))

(defn exit
  "Exit from the app with exit status"
  ([status]
   (System/exit status))
  ([status msg]
   (println msg)
   (exit status)))

(defn get-license-type-totals
  "Return a frequency map of license types as keys and license types as values"
  [licenses]
  (let [freqs (frequencies (map #(:type (:license %)) licenses))
        ordered-freqs (into (sorted-map) freqs)]
    ordered-freqs))

(defn process-deps
  "Print parsed dependencies pretty"
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
        (report/print-license-header options))

      (doseq [line licenses]
        (println (report/format-license line options))))

    (when with-totals-opt
      (println))

    (when show-totals
      (when table-headers
        (report/print-totals-header options))

      (doseq [[license-type freq] totals]
        (println (report/format-total license-type freq options))))

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
        "pip-license-checker -r resources/requirements.txt --rate-limits 10/1000"
        "pip-license-checker -r resources/requirements.github.txt --github-token your-token"
        "pip-license-checker -x resources/external.csv --exclude-license '(?i).*(?:mit|bsd).*'"
        "pip-license-checker -x resources/external.csv --external-options '{:skip-header false}'"
        "pip-license-checker -x resources/external.cocoapods --external-format cocoapods'"
        "pip-license-checker -x resources/external.edn --external-format edn --formatter '%-70s %-60s %-35s'"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing command arguments:\n"
       (str/join \newline errors)))

(defn parse-rate-limits
  "Parse rate limits CLI option"
  [limits-str]
  (let [split (-> limits-str (str/split #"/"))
        parsed (->> split (map #(Integer/parseInt %)))
        result {:requests (first parsed) :millis (second parsed)}]
    result))

(defn validate-rate-limits
  "Validate rale limits CLI option"
  [limits-map]
  (->>
   limits-map
   vals
   (every? #(and (some? %) (pos? %)))))

(def rate-limits-msg
  "Rate limits must be positive integers in format REQUESTS/MILLISECONDS")

(def cli-options
  [["-v" "--verbose" "Make output verbose" :default false]
   ["-r" "--requirements REQUIREMENTS_FILE" "Python pip requirement file name"
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
   [nil "--formatter PRINTF_FMT" "Printf-style formatter string for report formatting"
    :default report/table-formatter
    :validate [report/valid-formatter? report/invalid-formatter]]
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
   [nil "--rate-limits REQUESTS/MILLISECONDS" "Rate limit requests to public APIs"
    :default {:requests 120 :millis 60000}
    :parse-fn parse-rate-limits
    :validate [validate-rate-limits rate-limits-msg]]
   [nil "--github-token TOKEN" "GitHub OAuth Token to increase rate-limits. Defaults to GITHUB_TOKEN env"
    :default (System/getenv "GITHUB_TOKEN")]
   ["-h" "--help" "Print this help message"]])

(defn extend-fail-opt
  "Try to substitute common fail option in set with specific parts it consist of"
  [fail-opts]
  (if (contains? fail-opts license/type-copyleft-all)
    (clojure.set/difference
     (clojure.set/union fail-opts license/types-copyleft)
     #{license/type-copyleft-all})
    fail-opts))

(defn post-process-options
  "Update option map"
  [options]
  (let [opts' (dissoc options :requirements :external)
        fail-opt (:fail opts')
        fail-opt-exteded (extend-fail-opt fail-opt)
        updated-opts (assoc opts' :fail fail-opt-exteded)]
    updated-opts))

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
      (process-deps packages requirements external options))))
