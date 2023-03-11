;; Copyright © 2020-2023 Vitaly Samigullin
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
   [pip-license-checker.data :as d]
   [pip-license-checker.external :as external]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.license :as license]
   [pip-license-checker.logging :as logging]
   [pip-license-checker.pypi :as pypi]
   [pip-license-checker.report :as report]))

;; helpers

(defn exit
  "Exit from the app with exit status"
  ([status]
   (System/exit status))
  ([status msg]
   (println msg)
   (exit status)))

;; report generation and representation

(defn get-totals
  "Return a map of license types as keys and frequencies as values"
  [licenses]
  (->> licenses
       (map #(get-in % [:license :type]))
       frequencies
       (into (sorted-map))))

(defn repack-dep
  "Remove unused keys from dependency record"
  [dep options]
  (let [misc (logging/format-logs (:logs dep) options)]
    (d/map->ReportItem
     {:dependency (select-keys (:requirement dep) [:name :version])
      :license (select-keys (:license dep) [:name :type])
      :misc misc})))

(defn get-deps
  "Get a list of dependencies from various sources"
  [arguments]
  (let [{:keys [packages requirements external options]} arguments
        python-deps (pypi/get-parsed-deps packages requirements options)
        external-deps (external/get-parsed-deps external options)
        deps (concat python-deps external-deps)
        filtered (filters/filter-parsed-deps deps options)]
    (map #(repack-dep % options) filtered)))

(defn get-report
  "Get a formatted report"
  [items options]
  (let [totals (get-totals items)
        fails (->> totals
                   keys
                   (into (sorted-set))
                   (intersection (:fail options))
                   seq)]
    (d/map->Report {:headers report/report-headers
                    :items items
                    :totals totals
                    :fails fails})))

(defn shutdown
  "Shutdown the app gracefully"
  [report options]
  (let [{parallel-opt :parallel exit-opt :exit} options
        {fails :fails} report]
    ;; shutdown a thread pool used by pmap to allow JVM shutdown
    ;; pmap is used only with --parallel option
    (when exit-opt
      (when parallel-opt
        (shutdown-agents))
      (when fails
        (exit 1)))))

;; cli args

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
        "pip-license-checker --totals --headers --requirements resources/requirements.txt"
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
  [["-v" "--verbose" "Verbosity level: error (-v), info (-vv), debug (-vvv)"
    :default 0
    :update-fn inc]
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
   [nil "--report-format FORMAT" "Report format: stdout, json, json-pretty, csv"
    :default report/format-stdout
    :validate [report/valid-format? report/invalid-format]]
   [nil "--formatter PRINTF_FMT" "Printf-style formatter string for stdout report formatting"
    :default report/report-formatter
    :validate [report/valid-formatter? report/invalid-formatter]]
   ["-f" "--fail LICENSE_TYPE" "Return non-zero exit code if license type is found"
    :default (sorted-set)
    :multi true
    :update-fn conj
    :validate [license/is-type-valid? license/invalid-type]]
   ["-e" "--exclude REGEX" "PCRE to exclude packages with matching names" :parse-fn #(re-pattern %)]
   [nil "--exclude-license REGEX" "PCRE to exclude packages with matching license names" :parse-fn #(re-pattern %)]
   [nil "--[no-]pre" "Include pre-release and development versions. By default, use only stable versions" :default false]
   [nil "--[no-]totals" "Print totals for license types" :default false]
   [nil "--[no-]with-totals" "[deprecated '0.41.0'] Print totals for license types" :default nil]
   [nil "--[no-]totals-only" "Print only totals for license types" :default false]
   [nil "--[no-]headers" "Print report headers" :default false]
   [nil "--[no-]table-headers" "[deprecated '0.41.0'] Print table headers" :default nil]
   [nil "--[no-]fails-only" "Print only packages of license types specified with --fail flags" :default false]
   [nil "--[no-]parallel" "Run requests in parallel" :default true]
   [nil "--[no-]exit" "Exit program, used for CLI mode" :default true]
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

(defn assoc-if
  "Add key-value to a map if value is not nil"
  [coll key value]
  (if (some? value) (assoc coll key value) coll))

(defn process-deprecated-options
  "Route deprecated options to support backward compatibility"
  [options]
  (let [{:keys [with-totals table-headers]} options  ;; deprecated
        {:keys [totals headers]} options  ;; new ones
        totals-opt (if (some? with-totals) with-totals totals)
        headers-opt (if (some? table-headers) table-headers headers)
        opts' (dissoc options :with-totals :table-headers :totals :headers)
        opts'' (assoc-if opts' :totals totals-opt)
        opts''' (assoc-if opts'' :headers headers-opt)]
    opts'''))

(defn update-options
  "Update options map"
  [options]
  (let [opts' (dissoc options :requirements :external)
        fail-opt (:fail opts')
        fail-opt-exteded (extend-fail-opt fail-opt)]
    (-> opts'
        (assoc :fail fail-opt-exteded)
        process-deprecated-options)))

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
       :options (update-options options)}
      :else
      {:exit-message (usage summary)})))

;; entrypoint

(defn -main
  "App entry point"
  [& args]
  (let [arguments (validate-args args)
        {:keys [options exit-message ok?]} arguments]
    (when exit-message
      (exit (if ok? 0 1) exit-message))

    (-> arguments
        get-deps
        (get-report options)
        (report/format-report options)
        (shutdown options))))
