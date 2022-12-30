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

(ns pip-license-checker.pypi
  "Python PyPI API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [indole.core :refer [make-rate-limiter]]
   [pip-license-checker.data :as d]
   [pip-license-checker.exception :as exception]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.github :as github]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
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

(defn api-request-releases
  "DEPRECATED: Use `api-simple-request-releases` instead

  Moved out as a standalone function for testing simplicity"
  [url rate-limiter]
  (http/request-get url settings-http-client rate-limiter))

(defn api-get-releases
  "DEPRECATED: Use `api-simple-get-releases` instead

  Get seq of versions available for a package
  NB! versions are not sorted!"
  [package-name rate-limiter]
  (let [url (str/join "/" [pypi-json-api-url package-name "json"])
        resp (try (api-request-releases url rate-limiter)
                  (catch Exception e e))
        error (when (instance? Exception resp)
                (exception/get-error-message "PyPI::releases" resp))
        data (when (nil? error) resp)
        versions (-> data
                     :body
                     json/parse-string
                     (get "releases")
                     keys)
        releases (->> versions
                      (map #(version/parse-version %))
                      (filter #(not (nil? %))))]
    releases))

(defn api-simple-request-releases
  "Moved out as a standalone function for testing simplicity"
  [url rate-limiter]
  (let [settings (merge settings-http-client pypi-simple-api-headers)]
    (http/request-get url settings rate-limiter)))

(defn api-simple-get-releases
  "Get seq of versions available for a project in PyPI Simple API"
  [package-name rate-limiter]
  (let [url (str/join "/" [pypi-simple-api-url package-name])
        resp (try (api-simple-request-releases url rate-limiter)
                  (catch Exception e e))
        error (when (instance? Exception resp)
                (exception/get-error-message "PyPI::releases" resp))
        data (when (nil? error) resp)
        files (-> data
                  :body
                  json/parse-string
                  (get "files"))
        versions (->>
                  files
                  (filter #(= (get % "yanked") false))
                  (map #(version/get-dist-version (get % "filename") package-name))
                  distinct)
        releases (->>
                  versions
                  (map #(version/parse-version %))
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
        error (when (instance? Exception resp)
                (exception/get-error-message "PyPI::project" resp))
        resp-data (when (nil? error) resp)
        requirement-with-version
        (d/map->Requirement {:name name
                             :version (or version (:orig (last (first specifiers))))
                             :specifiers specifiers})]
    (cond
      (and resp-data version)
      (d/map->PyPiProject {:status req-status-found
                           :requirement requirement-with-version
                           :api-response
                           (->
                            resp-data
                            :body
                            json/parse-string)
                           :license nil
                           :error error})
      error
      (d/map->PyPiProject {:status req-status-error
                           :requirement requirement-with-version
                           :api-response nil
                           :license (license/get-license-error nil)
                           :error error})
      (nil? version)
      (d/map->PyPiProject {:status req-status-error
                           :requirement requirement-with-version
                           :api-response nil
                           :license (license/get-license-error nil)
                           :error (format "PyPI::version Not found")}))))

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
        {:strs [license classifiers home_page]} info
        license-license (if (contains? license-undefined license) nil license)
        classifiers-license (classifiers->license classifiers)
        name (or
              classifiers-license
              license-license)
        gh-license (when (nil? name) (github/homepage->license home_page options rate-limiter))
        gh-error (:error gh-license)
        license-name (or name (:name gh-license))
        license (license/license-with-type license-name)
        error-chain (exception/join-ex-info (:error license) gh-error)
        result (d/->License (:name license) (:type license) error-chain)]
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
        result (d/->Requirement package-name nil specifiers)]
    result))

(defn requirement->dep
  "Return dependency object"
  [requirement-rec options rate-limiter]
  (let [resp-data (api-get-project requirement-rec options rate-limiter)
        {:keys [status requirement api-response error]} resp-data
        license (if (= status req-status-error)
                  (d/->License license/name-error license/type-error nil)
                  (api-response->license-map api-response options rate-limiter))
        error-chain (exception/join-ex-info error (:error license))
        project (d/map->Dependency
                 {:requirement requirement
                  :license license
                  :error error-chain})]
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
