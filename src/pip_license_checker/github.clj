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

(ns pip-license-checker.github
  "GitHub API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [pip-license-checker.http :as http]
   [pip-license-checker.license :as license]
   [clojure.string :as str]))

(def url-github-base "https://api.github.com/repos")

(def logger-github "GitHub")

(def settings-http-client
  {:socket-timeout 1000
   :connection-timeout 1000
   :max-redirects 3})

(defn get-headers
  [options]
  (let [token (:github-token options)]
    (when token
      {:headers {"Authorization" (format "Bearer %s" token)}})))

(defn get-error-message
  "Get error message from GitHub API"
  [resp]
  (let [data (ex-data resp)
        error (format
               "[%s] %s %s"
               logger-github
               (:status data)
               (:reason-phrase data))]
    error))

(defn api-get-license
  "Get response from GitHub API"
  [path-parts options rate-limiter]
  (let [[_ owner repo] path-parts
        url (str/join "/" [url-github-base owner repo "license"])
        settings (merge settings-http-client (get-headers options))
        resp (try
               (http/request-get url settings rate-limiter)
               (catch Exception e e))
        error (when (instance? Exception resp) (get-error-message resp))
        resp-data (when (nil? error) resp)
        license-name (-> resp-data
                         :body
                         json/parse-string
                         (get-in ["license" "name"]))]
    (license/map->License {:name license-name :type nil :error error})))

(defn homepage->license
  "Get license name from homepage if it's a GitHub URL"
  [url options rate-limiter]
  (let [url-sanitized (if url (str/replace url #"/$" "") nil)
        github-url
        (try (re-find #"^(?:https://github.com)/(.*)/(.*)" url-sanitized)
             (catch Exception _ nil))
        url-valid? (= 3 (count github-url))
        license (if url-valid?
                  (api-get-license github-url options rate-limiter)
                  (license/->License nil nil nil))]
    license))
