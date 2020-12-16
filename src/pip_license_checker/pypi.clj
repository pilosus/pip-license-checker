(ns pip-license-checker.pypi
  "Python PyPI API functions"
  (:gen-class)
  (:require
   [cheshire.core :as json]
   [clj-http.client :as http]
   [clojure.string :as str]
   [pip-license-checker.filters :as filters]))


(def settings-http-client
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})

(def url-pypi-base "https://pypi.org/pypi")

(def license-name-error "Error")
(def license-desc-error "???")

(def license-desc-copyleft "Copyleft")
(def license-desc-permissive "Permissive")
(def license-desc-other "Other")

(def license-data-error {:name license-name-error :desc license-desc-error})

(def regex-match-classifier #"License :: .*")
(def regex-split-classifier #" :: ")

(def regex-ignore-case #"(?i)")

;; Copyleft/Permissive/Other description is based on the assumption
;; a requirement is used as a library. Conditions for linking a library
;; to the code licensed under a different license determine it's permissiveness.
;; See more:
;; https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licences
(def licenses-copyleft
  "Free software licenses (Copyleft)"
  [
   #"^Affero"
   #"^EUPL"
   #"European Union Public Licence"
   #"^FDL"
   #"GNU Affero General Public License"
   #"GNU Free Documentation License"
   #"GNU General Public License"
   #"^GPL"
   #"IBM Public License"
   #"^MPL"
   #"Mozilla Public License"
   #"^OSL"
   #"Open Software License"])

(def licenses-permissive
  "Permissive licenses"
  [
   #"CeCILL-B Free Software License Agreement"
   #"^CeCILL-B"
   #"License :: CeCILL-C Free Software License Agreement"
   #"^CeCILL-C"
   #"CEA CNRS Inria Logiciel Libre License"
   #"^CeCILL-2.1"  ;; http://cecill.info/index.en.html
   #"Academic Free License"
   #"^AFL"
   #"Apache Software License"
   #"^Apache"
   #"BSD License"
   #"^BSD"
   #"Historical Permission Notice and Disclaimer"
   #"^HPND"
   #"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"^LGPL"
   #"MIT License"
   #"^MIT"
   #"ISC License"
   #"^ISCL"
   #"Python Software Foundation License"
   #"Python License"
   #"Unlicense"
   #"Universal Permissive License"
   #"UPL"
   #"W3C License"
   #"W3C"
   #"zlib/libpng License"
   #"zlib/libpng"
   #"Public Domain"
   ])

;; Get API response, parse it

(defn get-requirement-response
  "Return respone of GET request to PyPI API for requirement"
  [requirement]
  (let [{:keys [name version] :as origin} requirement
        url
        (if (= version filters/version-latest)
          (str/join "/" [url-pypi-base name "json"])
          (str/join "/" [url-pypi-base name version "json"]))
        response (try (http/get url settings-http-client) (catch Exception e nil))]
    (if response
      {:ok? true :requirement origin :response (:body response)}
      {:ok? false :requirement origin})))

(defn requirement-response->data
  "Return hash-map from PyPI API JSON response"
  [response]
  (let [{:keys [ok? response requirement]} response]
    (if ok?
      {:ok? true :requirement requirement :data (json/parse-string response)}
      {:ok? false :requirement requirement})))

;; Helpers to get license name and description

(defn classifiers->license
  "Get first license name from PyPI trove classifiers list"
  [classifiers]
  (let [classifier
        (some #(if (re-matches regex-match-classifier % ) %) classifiers)
        name
        (if classifier (last (str/split classifier regex-split-classifier)) nil)]
    name))

(defn strings->pattern
  "Get regex pattern from sequence of strings"
  [patterns]
  (re-pattern
   (str regex-ignore-case
        (apply str (interpose "|" (map #(str "(?:" % ")") patterns))))))

(defn license-name->desc
  "Get license description by its name"
  [name]
  (let [regex-copyleft (strings->pattern licenses-copyleft)
        match-copyleft (some? (re-find regex-copyleft name))
        regex-permissive (strings->pattern licenses-permissive)
        match-permissive (some? (re-find regex-permissive name))]
    (cond
      match-copyleft license-desc-copyleft
      match-permissive license-desc-permissive
      :else license-desc-other)))

(defn data->license-map
  "Get license name from info.classifiers or info.license field of PyPI API data
  TODO extend with GitHub API response if no license found"
  [data]
  (let [info (get data "info")
        {:strs [license classifiers]} info
        classifiers-license (classifiers->license classifiers)
        ;; TODO add GitHub API check here
        license-name (or classifiers-license license)
        license-desc
        (license-name->desc (or license-name license-name-error))]
    (if license-name
      {:name license-name :desc license-desc}
      license-data-error)))

;; Get license data from API JSON

(defn data->license
  "Return hash-map with license data"
  [json-data]
  (let [{:keys [ok? requirement data] :as origin} json-data]
    (if ok?
      {:ok? true
       :requirement requirement
       :license (data->license-map data)}
      {:ok? false
       :requirement requirement
       :license license-data-error})))

;; Entrypoint

(defn requirement->license
  "Return license hash-map for requirement"
  [requirement]
  (let [resp (get-requirement-response requirement)
        data (requirement-response->data resp)
        license (data->license data)]
    license))
