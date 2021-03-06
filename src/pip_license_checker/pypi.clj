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
   [pip-license-checker.version :as version]))

(def settings-http-client
  {:socket-timeout 3000
   :connection-timeout 3000
   :max-redirects 3})

(def url-pypi-base "https://pypi.org/pypi")

(def license-name-error "Error")

(def license-desc-error "Error")
(def license-desc-copyleft "Copyleft")
(def license-desc-permissive "Permissive")
(def license-desc-other "Other")

(def license-types
  (sorted-set license-desc-error
              license-desc-copyleft
              license-desc-permissive
              license-desc-other))

(def invalid-license-type
  (format "Invalid license type. Use one of: %s"
          (str/join ", " license-types)))

(def license-data-error {:name license-name-error :desc license-desc-error})

(def license-undefined #{"" "UNKNOWN" [] ["UNKNOWN"]})
(def unspecific-license-classifiers #{"License :: OSI Approved"})

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
  [#"^Affero"
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
  [#"CeCILL-B Free Software License Agreement"
   #"^CeCILL-B"
   #"^CeCILL-C"
   #"CEA CNRS Inria Logiciel Libre License"
   #"^CeCILL-2.1"  ;; http://cecill.info/index.en.html
   #"Academic Free License"
   #"^AFL"
   #"Apache Software License"
   #"^Apache"
   #"Artistic"
   #"BSD"
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
   #"Zope Public License"
   #"zlib/libpng License"
   #"zlib/libpng"
   #"Public Domain"])

;; Helpers

(defn is-license-type-valid?
  "Return true if license-type string is valid, false otherwise"
  [license-type]
  (contains? license-types license-type))

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
        (license-name->desc (or license-name license-name-error))]
    (if license-name
      {:name license-name :desc license-desc}
      license-data-error)))

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
       :license license-data-error})))

;; Entrypoint

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

;;
;; Instrumented functions - uncomment only while testing
;;

;; (instrument `get-requirement-version)
;; (instrument `requirement-response->data)
;; (instrument `data->license)
;; (instrument `requirement->license)
