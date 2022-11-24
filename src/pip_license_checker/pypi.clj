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

(ns pip-license-checker.pypi
  "Python PyPI API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [indole.core :refer [make-rate-limiter]]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.github :as github]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
   [pip-license-checker.version :as version]
))

(def settings-http-client
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})

(def url-pypi-base "https://pypi.org/pypi")

(def license-undefined #{"" "UNKNOWN" [] ["UNKNOWN"]})
(def unspecific-license-classifiers #{"License :: OSI Approved"})

(def regex-match-classifier #"License :: .*")
(def regex-split-classifier #" :: ")

(def regex-split-specifier-ops #"(===|==|~=|!=|>=|<=|<|>)")

;; Data structures

(defrecord Requirement [name
                        version
                        specifiers])

(defrecord PyPiProject [ok?
                        ^Requirement requirement
                        api-response
                        license])

;; Get API response, parse it

(defn api-get-releases
  "Get seq of versions available for a package
  NB! versions are not sorted!"
  [package-name rate-limiter]
  (let [url (str/join "/" [url-pypi-base package-name "json"])
        response (try (http/request-get url settings-http-client rate-limiter)
                      (catch Exception _ nil))
        body (:body response)
        data (json/parse-string body)
        releases (get data "releases")
        versions (keys releases)
        versions-parsed (map #(version/parse-version %) versions)
        versions-valid (filter #(not (nil? %)) versions-parsed)]
    versions-valid))

(defn api-get-requirement-version
  "Return respone of GET request to PyPI API for requirement"
  [requirement options rate-limiter]
  (let [{:keys [name specifiers]} requirement
        pre (:pre options)
        versions (api-get-releases name rate-limiter)
        version (version/get-version specifiers versions :pre pre)
        url
        (if (= version nil)
          (str/join "/" [url-pypi-base name "json"])
          (str/join "/" [url-pypi-base name version "json"]))
        response (try
                   (http/request-get url settings-http-client rate-limiter)
                   (catch Exception _ nil))
        requirement-with-version
        (map->Requirement {:name name
                           :version (or version (:orig (last (first specifiers))))
                           :specifiers specifiers})]
    (if (and response version)
      (map->PyPiProject {:ok? true
                         :requirement requirement-with-version
                         :api-response
                         (->
                          response
                          :body
                          json/parse-string)
                         :license nil})
      (map->PyPiProject {:ok? false
                         :requirement requirement-with-version
                         :api-response nil
                         :license nil}))))

;; Helpers to get license name and description

(defn classifiers->license
  "Get first most detailed license name from PyPI trove classifiers list"
  [classifiers]
  (let [license-classifiers
        (filter #(re-matches regex-match-classifier %) classifiers)
        specifict-classifiers
        (remove #(contains? unspecific-license-classifiers %) license-classifiers)
        license-names
        (map #(last (str/split % regex-split-classifier)) specifict-classifiers)
        classifier (str/join ", " license-names)
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
              license-license
              (github/homepage->license-name home_page options rate-limiter))
        license (license/name->type name)]
    license))

;; Get license data from API JSON

(defn requirement->rec
  "Parse requirement string into map with package name and its specifiers parsed"
  [requirement-line]
  (let [package-name (first (str/split requirement-line regex-split-specifier-ops))
        specifiers-str (subs requirement-line (count package-name))
        specifiers-vec (version/parse-specifiers specifiers-str)
        specifiers (if (= specifiers-vec [nil]) nil specifiers-vec)
        result (->Requirement package-name nil specifiers)]
    result))

(defn requirement-rec->project-with-license
  "Return license hash-map for requirement"
  [requirement-rec options rate-limiter]
  (let [api-response (api-get-requirement-version requirement-rec options rate-limiter)
        {:keys [ok? requirement api-response]} api-response
        project
        (if ok?
          (map->PyPiProject {:ok? true
                             :requirement requirement
                             :api-response api-response
                             :license
                             (api-response->license-map api-response options rate-limiter)})
          (map->PyPiProject {:ok? false
                             :requirement requirement
                             :api-response api-response
                             :license license/data-error}))]
    project))

(defn get-all-requirements
  "Get a sequence of all requirements"
  [packages requirements]
  (let [file-packages (file/get-requirement-lines requirements)]
    (concat packages file-packages)))

;; Entrypoint

(defn get-parsed-requiements
  "Apply filters and get verdicts for all requirements"
  [packages requirements options]
  (let [exclude-pattern (:exclude options)
        rate-limiter (make-rate-limiter
                      (or (get-in options [:rate-limits :millis]) 60000)
                      (or (get-in options [:rate-limits :requests]) 120))
        licenses (->> (get-all-requirements packages requirements)
                      (filters/remove-requirements-internal-rules)
                      (filters/remove-requirements-user-rules exclude-pattern)
                      (map filters/sanitize-requirement)
                      (map requirement->rec)
                      (pmap #(requirement-rec->project-with-license % options rate-limiter)))]
    licenses))
