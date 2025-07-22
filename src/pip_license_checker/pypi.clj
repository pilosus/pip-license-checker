;; Copyright © Vitaly Samigullin
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

(ns pip-license-checker.pypi
  "Python PyPI API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [indole.core :refer [make-rate-limiter]]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.github :as g]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
   [pip-license-checker.logging :as l]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.version :as version]))

(def settings-http-client
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})

(def pypi-simple-api-headers
  {:headers {"Accept" "application/vnd.pypi.simple.v1+json"}})

(def pypi-json-api-url "https://pypi.org/pypi")
(def pypi-simple-api-url "https://pypi.org/simple")

(def license-undefined #{"" "UNKNOWN" [] ["UNKNOWN"]})
(def unspecific-license-classifiers #{"License :: OSI Approved"})

(def regex-match-classifier #"License :: .*")
(def regex-split-classifier #" :: ")

(def regex-split-specifier-ops #"(===|==|~=|!=|>=|<=|<|>)")

(def req-status-found :found)
(def req-status-error :error)

;; Get API response, parse it

(defn api-simple-request-releases
  "Moved out as a standalone function for testing simplicity"
  [url rate-limiter]
  (let [settings (merge settings-http-client pypi-simple-api-headers)]
    (http/request-get url settings rate-limiter)))

(defn get-version-and-meta
  "Get version and release meta"
  [file-obj package-name]
  (let [version (version/get-dist-version
                 (get file-obj "filename")
                 package-name)
        yanked (if (= (get file-obj "yanked" false) false) false true)
        result [version {:yanked yanked}]]
    result))

(defn api-simple-get-releases
  "Get seq of versions available for a project in PyPI Simple API"
  [package-name rate-limiter]
  (let [url (str/join "/" [pypi-simple-api-url package-name])
        resp (try (api-simple-request-releases url rate-limiter)
                  (catch Exception e e))
        logs (when (instance? Exception resp)
               [{:level :error
                 :name "PyPI::releases"
                 :message (l/get-error-message resp)}])
        data (when (nil? logs) resp)
        files (-> data
                  :body
                  json/parse-string
                  (get "files"))
        releases (->>
                  files
                  (map #(get-version-and-meta % package-name))
                  distinct
                  (map #(apply version/parse-version %))
                  (filter #(not (nil? %))))]
    releases))

(defn api-request-project
  "Moved out as a standalone function for testing simplicity"
  [url rate-limiter]
  (http/request-get url settings-http-client rate-limiter))

(defn api-get-project
  "Return respone of GET request to PyPI API for requirement"
  [requirement options rate-limiter]
  (let [{:keys [name specifiers]} requirement
        releases (api-simple-get-releases name rate-limiter)
        version (version/get-version specifiers releases :pre (:pre options))
        url
        (if (nil? version)
          (str/join "/" [pypi-json-api-url name "json"])
          (str/join "/" [pypi-json-api-url name version "json"]))
        resp (try
               (api-request-project url rate-limiter)
               (catch Exception e e))
        logs (when (instance? Exception resp)
               [{:level :error
                 :name "PyPI::project"
                 :message (l/get-error-message resp)}])
        resp-data (when (nil? logs) resp)
        requirement-with-version
        (s/assert
         ::sp/requirement
         {:name name
          :version (or version (:orig (last (first specifiers))))
          :specifiers specifiers})]
    (cond
      (and resp-data version)
      (s/assert
       ::sp/pypi-project
       {:status req-status-found
        :requirement requirement-with-version
        :api-response
        (->
         resp-data
         :body
         json/parse-string)
        :license nil
        :logs logs})
      logs
      (s/assert
       ::sp/pypi-project
       {:status req-status-error
        :requirement requirement-with-version
        :api-response nil
        :license (license/get-license-error nil)
        :logs logs})
      (nil? version)
      (s/assert
       ::sp/pypi-project
       {:status req-status-error
        :requirement requirement-with-version
        :api-response nil
        :license (license/get-license-error nil)
        :logs [{:level :error
                :name "PyPI::version"
                :message "Not found"}]}))))

;; Helpers to get license name and description

(defn classifiers->license
  "Get first most detailed license name from PyPI trove classifiers list"
  [classifiers]
  (let [classifier (->> classifiers
                        (filter #(re-matches regex-match-classifier %))
                        (remove #(contains? unspecific-license-classifiers %))
                        (map #(last (str/split % regex-split-classifier)))
                        (str/join ", "))
        result (if (= classifier "") nil classifier)]
    result))

(defn api-response->license-map
  "Get license name from info.classifiers or info.license field of PyPI API data"
  [api-response options rate-limiter]
  (let [info (get api-response "info")
        {:strs [license_expression license classifiers home_page]} info
        ;; license expression has higher priority as license
        license (or license_expression license)
        license-license (if (contains? license-undefined license) nil license)
        classifiers-license (classifiers->license classifiers)
        name (or
              classifiers-license
              license-license)
        gh-license (when (nil? name)
                     (g/homepage->license home_page options rate-limiter))
        gh-info-logs (when (:name gh-license)
                       [{:level :info
                         :name "Checker"
                         :message "Fallback to GitHub for non-version-specific license"}])
        license-name (or name (:name gh-license))
        license (license/license-with-type license-name)
        logs (not-empty (concat (:logs license) (:logs gh-license) gh-info-logs))
        result (s/assert
                ::sp/license
                {:name (:name license)
                 :type (:type license)
                 :logs logs})]
    result))

;; Get license data from API JSON

(defn requirement->rec
  "Parse requirement string into map with package name and its specifiers parsed"
  [requirement-line]
  (let [package-name (-> requirement-line
                         (str/split regex-split-specifier-ops)
                         first)
        specifiers-str (subs requirement-line (count package-name))
        specifiers-vec (version/parse-specifiers specifiers-str)
        specifiers (if (= specifiers-vec [nil]) nil specifiers-vec)
        result (s/assert
                ::sp/requirement
                {:name package-name
                 :version nil
                 :specifiers specifiers})]
    result))

(defn requirement->dep
  "Return dependency object"
  [requirement-rec options rate-limiter]
  (let [resp-data (api-get-project requirement-rec options rate-limiter)
        {:keys [status requirement api-response logs]} resp-data
        license (if (= status req-status-error)
                  (s/assert
                   ::sp/license
                   {:name license/name-error
                    :type license/type-error
                    :logs nil})
                  (api-response->license-map api-response options rate-limiter))
        project (s/assert
                 ::sp/dependency
                 {:requirement requirement
                  :license license
                  :logs (not-empty (concat (:logs license) logs))})]
    project))

(defn get-all-requirements
  "Get a sequence of all requirements"
  [packages requirements]
  (let [file-packages (file/get-requirement-lines requirements)]
    (concat packages file-packages)))

;; Entrypoint

(defn get-parsed-deps
  "Apply filters and get verdicts for all deps"
  [packages requirements options]
  (let [exclude-pattern (:exclude options)
        map-fn (if (:parallel options) pmap map)
        rate-limiter (make-rate-limiter
                      (or (get-in options [:rate-limits :millis]) 60000)
                      (or (get-in options [:rate-limits :requests]) 120))
        licenses (->> (get-all-requirements packages requirements)
                      (filters/remove-requirements-internal-rules)
                      (filters/remove-requirements-user-rules exclude-pattern)
                      (map filters/sanitize-requirement)
                      (map requirement->rec)
                      (map-fn #(requirement->dep
                                %
                                options
                                rate-limiter)))]
    licenses))
