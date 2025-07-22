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

(ns pip-license-checker.logging
  (:gen-class)
  (:require
   [clojure.string :as str]))

(def log-levels {:debug 10 :info 20 :error 30 :silent 100})

(def verbosity-to-levels
  {0 :silent
   1 :error
   2 :info
   3 :debug})

(defn get-ex-info
  "Generate a human-readable exception message"
  [ex]
  (let [name (-> ex .getClass .getSimpleName)
        message (ex-message ex)]
    (format "%s %s" name message)))

(defn get-error-message
  "Format error message for a clj-http error response"
  [resp]
  (if-let [data (ex-data resp)]
    (format "%s %s" (:status data) (:reason-phrase data))
    (get-ex-info resp)))

(defn format-log-item
  "Format single log item to a string"
  [log]
  (let [level-str (-> log :level name str/capitalize)]
    (format "%s: %s %s" level-str (:name log) (:message log))))

(defn assoc-level-number
  "Substitute level keyword with corresponsing level number"
  [log]
  (assoc log :number (get log-levels (:level log))))

(defn get-log-level-number
  "Get log level number for a given verbosity number from CLI options"
  [options]
  (let [verbosity (min (get options :verbose 0)
                       (apply max (keys verbosity-to-levels)))
        level (->> verbosity
                   (get verbosity-to-levels)
                   (get log-levels))]
    level))

(defn format-logs
  "Format sequense of logs to a string"
  [logs options]
  (let [level (get-log-level-number options)
        sorted (->>
                logs
                (remove nil?)
                (map assoc-level-number)
                (filter #(>= (get % :number) level))
                distinct
                (sort-by :number)
                reverse
                (map format-log-item)
                (str/join "\n"))]
    sorted))
