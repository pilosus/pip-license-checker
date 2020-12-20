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
