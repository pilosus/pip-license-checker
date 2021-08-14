(ns pip-license-checker.pypi
  "Python PyPI API functions"
  (:gen-class)
  (:require
   ;;[clojure.spec.test.alpha :refer [instrument]]
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [pip-license-checker.github :as github]
   [pip-license-checker.spec :as sp]
   [pip-license-checker.file :as file]
   [pip-license-checker.filters :as filters]
   [pip-license-checker.version :as version]
   [pip-license-checker.license :as license]))

(def settings-http-client
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})

(def url-pypi-base "https://pypi.org/pypi")

(def license-undefined #{"" "UNKNOWN" [] ["UNKNOWN"]})
(def unspecific-license-classifiers #{"License :: OSI Approved"})

(def regex-match-classifier #"License :: .*")
(def regex-split-classifier #" :: ")

;; Get API response, parse it

(defn get-releases
  "Get seq of versions available for a package
  NB! versions are not sorted!"
  [package-name]
  (let [url (str/join "/" [url-pypi-base package-name "json"])
        response (try (http/get url settings-http-client)
                      (catch Exception _ nil))
        body (:body response)
        data (json/parse-string body)
        releases (get data "releases")
        versions (keys releases)
        versions-parsed (map #(version/parse-version %) versions)
        versions-valid (filter #(not (nil? %)) versions-parsed)]
    versions-valid))

(s/fdef get-requirement-version
  :args (s/cat
         :requirement ::sp/requirement
         :pre (s/? keyword?)
         :value (s/? boolean?))
  :ret ::sp/requirement-response)

(defn get-requirement-version
  "Return respone of GET request to PyPI API for requirement"
  [requirement & {:keys [pre] :or {pre true}}]
  (let [{:keys [name specifiers]} requirement
        versions (get-releases name)
        version (version/get-version specifiers versions :pre pre)
        url
        (if (= version nil)
          (str/join "/" [url-pypi-base name "json"])
          (str/join "/" [url-pypi-base name version "json"]))
        response (try (http/get url settings-http-client) (catch Exception _ nil))
        origin {:name name
                :version (or version (:orig (last (first specifiers))))}]
    (if (and response version)
      {:ok? true :requirement origin :response (:body response)}
      {:ok? false :requirement origin})))

(s/fdef requirement-response->data
  :args ::sp/requirement-response
  :ret ::sp/requirement-response-data)

(defn requirement-response->data
  "Return hash-map from PyPI API JSON response"
  [response]
  (let [{:keys [ok? response requirement]} response]
    (if ok?
      {:ok? true :requirement requirement :data (json/parse-string response)}
      {:ok? false :requirement requirement})))

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

(defn data->license-map
  "Get license name from info.classifiers or info.license field of PyPI API data"
  [data]
  (let [info (get data "info")
        {:strs [license classifiers home_page]} info
        license-license (if (contains? license-undefined license) nil license)
        classifiers-license (classifiers->license classifiers)
        license-name (or
                      classifiers-license
                      license-license
                      (github/homepage->license-name home_page))
        license-desc
        (license/name->type (or license-name license/name-error))]
    (if license-name
      {:name license-name :type license-desc}
      license/data-error)))

;; Get license data from API JSON

(s/fdef data->license
  :args ::sp/requirement-response-data
  :ret ::sp/requirement-response-license)

(defn data->license
  "Return hash-map with license data"
  [json-data]
  (let [{:keys [ok? requirement data]} json-data]
    (if ok?
      {:ok? true
       :requirement requirement
       :license (data->license-map data)}
      {:ok? false
       :requirement requirement
       :license license/data-error})))

(s/fdef requirement->license
  :args (s/cat
         :requirement ::sp/requirement
         :pre (s/? keyword?)
         :value (s/? boolean?))
  :ret ::sp/requirement-response-license)

(defn requirement->license
  "Return license hash-map for requirement"
  [requirement & {:keys [pre]}]
  (let [resp (get-requirement-version requirement :pre pre)
        data (requirement-response->data resp)
        license (data->license data)]
    license))

(defn get-all-requirements
  "Get a sequence of all requirements"
  [packages requirements]
  (let [file-packages (file/get-requirement-lines requirements)]
    (concat packages file-packages)))

;; Entrypoint

(s/fdef get-parsed-requiements
  :args (s/cat
         :requirements ::sp/requirements-cli-arg
         :packages ::sp/packages-cli-arg
         :options ::sp/options-cli-arg)
  :ret (s/coll-of ::sp/requirement-response-license))

(defn get-parsed-requiements
  "Apply filters and get verdicts for all requirements"
  [packages requirements options]
  (let [exclude-pattern (:exclude options)
        pre (:pre options)
        licenses (->> (get-all-requirements packages requirements)
                      (filters/remove-requirements-internal-rules)
                      (filters/remove-requirements-user-rules exclude-pattern)
                      (map filters/sanitize-requirement)
                      (map filters/requirement->map)
                      (pmap #(requirement->license % :pre pre)))]
    licenses))

;;
;; Instrumented functions - uncomment only while testing
;;

;; (instrument `get-requirement-version)
;; (instrument `requirement-response->data)
;; (instrument `data->license)
;; (instrument `requirement->license)
