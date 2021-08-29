;; Copyright © 2020, 2021 Vitaly Samigullin
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
   [clj-http.client :as http]
   [clojure.string :as str]))

(def url-github-base "https://api.github.com/repos")

(def settings-http-client
  {:socket-timeout 1000
   :connection-timeout 1000
   :max-redirects 3})

(defn get-license-name
  "Get response from GitHub API"
  [path-parts]
  (let [[_ owner repo] path-parts
        url (str/join "/" [url-github-base owner repo "license"])
        response (try (http/get url settings-http-client) (catch Exception _ nil))
        data (if response (json/parse-string (:body response)) {})
        license-obj (get data "license")
        license-name (get license-obj "name")]
    license-name))

(defn homepage->license-name
  "Get license name from homepage if it is GitHub url"
  [url]
  (let [url-sanitized (if url (str/replace url #"/$" "") nil)
        github-url
        (try (re-find #"^(?:https://github.com)/(.*)/(.*)" url-sanitized)
             (catch Exception _ nil))
        is-github-url (= 3 (count github-url))
        license (if is-github-url (get-license-name github-url) nil)]
    license))
