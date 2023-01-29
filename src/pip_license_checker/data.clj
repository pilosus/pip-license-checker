;; Copyright © 2020-2023 Vitaly Samigullin
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

(ns pip-license-checker.data
  "Data structures and helper functions for them"
  (:gen-class))

(defrecord Log
    ;; Log representation
           [level     ;; keyword, one of: :error, :info, :debug
            name      ;; string of a logger name, e.g. "PyPI::version"
            message   ;; string
            ])

(defrecord License
           [name  ;; nilable String
            type  ;; nilable String
            logs  ;; nilable vector of Log
            ])

(defrecord Requirement
           [name       ;; nilable String
            version    ;; nilable String
            specifiers ;; nilable vector of vectors of format [op, version]; nil for non-Python deps
            ])

(defrecord PyPiProject
           ;; PyPI project as represented on https://pypi.org/project/<project-name>
           [status        ;; keyword
            requirement   ;; Requirement rec
            api-response  ;; nilable parsed JSON
            license       ;; License rec
            logs          ;; nilable vector of Log
            ])

(defrecord Dependency
           ;; General representation of dependency - PyPI project or external dep
           [requirement ;; Requirement rec
            license     ;; License rec
            logs        ;; nilable vector of Log
            ])

;; Processed entities

(defrecord ReportDependency
           [name    ;; nilable String
            version ;; nilable String
            ])

(defrecord ReportLicense
           [name ;; nilable String
            type ;; nilable String
            ])

(defrecord ReportItem
           [dependency ;; ReportDependency
            license    ;; ReportLicense
            misc       ;; nilable String
            ])

(defrecord ReportHeader
           [items  ;; list of String
            totals ;; list of String
            ])

(defrecord Report
           [headers ;; nilable list of ReportHeader
            items   ;; list of ReportItem
            totals  ;; nilalbe Map of String (license types): Integer (frequencies)
            fails   ;; nilable list of String (license types)
            ])
