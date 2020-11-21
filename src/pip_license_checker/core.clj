(ns pip-license-checker.core
  (:gen-class)
  (:require [clj-http.client :as http])
  (:require [cheshire.core :as json])
  (:require [clojure.walk :as walk]))

(defn 
  client-get-or-nil
  [url]
  (try
    (http/get url)
    (catch Exception e nil)))


(defn 
  get-package-response
  [name version]
  (let [base-url "https://pypi.org/pypi/"
        url (str base-url name "/" version "/json" )]
    (client-get-or-nil url)))


(defn 
  get-license
  [name version]
  (let [data (get-package-response name version)]
    (if (some? data) 
      ;; TODO refactor to a separate function, make fault tolerant
      (:license (:info (walk/keywordize-keys (json/parse-string (:body data)))))
      ("Error"))))


(defn -main
  [& args]
  (if (= (count args) 2) 
    (println (get-license (first args) (second args)))  
    ;; TODO move to separate usage function
    (println "Usage: pip_license_checker name version")))
