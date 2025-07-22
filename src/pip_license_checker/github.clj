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

(ns pip-license-checker.github
  "GitHub API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [pip-license-checker.http :as http]
   [pip-license-checker.logging :as l]
   [pip-license-checker.spec :as sp]))

(def url-github-base "https://api.github.com/repos")

(def header-github-api-version {"X-GitHub-API-Version" "2022-11-28"})

;; FIXME this is app-level error log
;; it should not be set by github module?
(def meta-not-found
  {:level :error
   :name "Checker"
   :message "License sources not found"})

(def settings-http-client
  {:socket-timeout 1000
   :connection-timeout 1000
   :max-redirects 3})

(defn get-headers
  [options]
  (let [token (:github-token options)
        header-token (when token {"Authorization" (format "Bearer %s" token)})
        headers (merge header-token header-github-api-version)]
    {:headers headers}))

(defn api-request-license
  "Moved out to a separate function for testing simplicity"
  [url settings rate-limiter]
  (http/request-get url settings rate-limiter))

(defn api-get-license
  "Get response from GitHub API"
  [path-parts options rate-limiter]
  (let [[_ owner repo] path-parts
        url (str/join "/" [url-github-base owner repo "license"])
        settings (merge
                  settings-http-client
                  (get-headers options))
        resp (try
               (api-request-license url settings rate-limiter)
               (catch Exception e e))
        logs (when (instance? Exception resp)
               [{:level :error
                 :name "GitHub::license"
                 :message (l/get-error-message resp)}])
        resp-data (when (nil? logs) resp)
        license-name (-> resp-data
                         :body
                         json/parse-string
                         (get-in ["license" "name"]))]
    (s/assert ::sp/license {:name license-name :type nil :logs logs})))

(defn homepage->license
  "Get license name from homepage if it's a GitHub URL"
  [url options rate-limiter]
  (let [url-sanitized (if url (str/replace url #"/$" "") nil)
        github-url (try
                     (re-find #"^(?:https://github.com)/(.*)/(.*)" url-sanitized)
                     (catch Exception _ nil))
        url-valid? (= 3 (count github-url))
        license (if url-valid?
                  (api-get-license github-url options rate-limiter)
                  (s/assert
                   ::sp/license
                   {:name nil :type nil :logs [meta-not-found]}))]
    license))
