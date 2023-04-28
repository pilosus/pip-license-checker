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

(ns pip-license-checker.spec
  "Specs"
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test.check.generators :as g]))

;;
;; Specs
;;
;; (require '[pip-license-checker.spec :as sp])
;; (s/valid? ::sp/requirement "abc")

;; Common

(s/def ::nilable-string (s/nilable string?))

;; Versions

(s/def ::version-str string?)
(s/def ::matched-version-part ::nilable-string)
(s/def ::non-negative-number (s/and number? (fn [n] (>= n 0))))
(s/def ::version-orig string?)
(s/def ::version-epoch ::non-negative-number)
(s/def ::version-release (s/coll-of number?))

(s/def ::opt-version-letter
  (s/nilable (s/tuple string? number?)))

(s/def ::opt-version-local
  (s/nilable (s/* (s/alt :letter string? :number number?))))

(s/def :version-meta/yanked boolean?)

(s/def ::version-meta
  (s/nilable
   (s/keys :req-un [:version-meta/yanked])))

(s/def :version/orig ::version-orig)
(s/def :version/epoch ::version-epoch)
(s/def :version/release ::version-release)
(s/def :version/pre ::opt-version-letter)
(s/def :version/post ::opt-version-letter)
(s/def :version/dev ::opt-version-letter)
(s/def :version/local ::opt-version-local)
(s/def :version/meta ::version-meta)

(s/def ::version
  (s/keys :req-un
          [:version/orig
           :version/epoch
           :version/release
           :version/pre
           :version/post
           :version/dev
           :version/local
           :version/meta]))

(s/def ::versions
  (s/nilable (s/coll-of ::version)))

;; Specifiers

(s/def ::specifier-str string?)

;; TODO must be rewritten with s/fspec or s/fdef?
;; not sure why this doesn't conform the spec
;; (s/def ::op (s/fspec :args (s/cat :a ::version :b ::version) :ret boolean?))
;; TODO minimal reproducable example of fns & specs to debug
(s/def ::op fn?)

(s/def ::specifier
  (s/nilable (s/tuple ::op ::version)))

(s/def ::specifiers
  (s/nilable (s/coll-of ::specifier)))

;; Logging

(s/def :log/level #{:error :info :debug})
(s/def :log/name string?)
(s/def :log/message string?)

(s/def ::log
  (s/keys :req-un
          [:log/level
           :log/name
           :log/message]))

(s/def ::logs (s/coll-of ::log))

;; License

(s/def :license/name ::nilable-string)
(s/def :license/type ::nilable-string)
(s/def :license/logs (s/nilable ::logs))

(s/def ::license
  (s/keys :req-un
          [:license/name
           :license/type
           :license/logs]))

;; Requirement

(s/def :requirement/name ::nilable-string)
(s/def :requirement/version ::nilable-string)
(s/def :requirement/specifiers (s/nilable ::specifiers))

(s/def ::requirement
  (s/keys
   :req-un
   [:requirement/name
    :requirement/version]
   :opt-un
   ;; specifiers not used for non-Python requirements
   [:requirement/specifiers]))

;; PyPI Project
;; as represented by JSON API reponse from
;; https://pypi.org/project/<project-name>

(s/def :pypi-project/status keyword?)
(s/def :pypi-project/requirement ::requirement)
(s/def :pypi-project/api-response (s/nilable map?))
(s/def :pypi-project/license (s/nilable ::license))
(s/def :pypi-project/logs (s/nilable ::logs))

(s/def ::pypi-project
  (s/keys :req-un
          [:pypi-project/status
           :pypi-project/requirement
           :pypi-project/api-response
           :pypi-project/license
           :pypi-project/logs]))

;; Dependency
;; General representation of dependency: PyPI project or external dep

(s/def :dependency/requirement ::requirement)
(s/def :dependency/license ::license)
(s/def :dependency/logs (s/nilable ::logs))

(s/def ::dependency
  (s/keys :req-un
          [:dependency/requirement
           :dependency/license
           :dependency/logs]))

;; Report elements

;; Report Dependency

(s/def :report-dependency/name ::nilable-string)
(s/def :report-dependency/version ::nilable-string)

(s/def ::report-dependency
  (s/keys :req-un
          [:report-dependency/name
           :report-dependency/version]))

;; Report License

(s/def :report-license/name ::nilable-string)
(s/def :report-license/type ::nilable-string)

(s/def ::report-license
  (s/keys :req-un
          [:report-license/name
           :report-license/type]))

;; Report Item

(s/def :report-item/dependency ::report-dependency)
(s/def :report-item/license ::report-license)
(s/def :report-item/misc ::nilable-string)

(s/def ::report-item
  (s/keys :req-un
          [:report-item/dependency
           :report-item/license
           :report-item/misc]))

;; Report Header

(s/def :report-header/items (s/coll-of string?))
(s/def :report-header/totals (s/coll-of string?))

(s/def ::report-header
  (s/keys :req-un
          [:report-header/items
           :report-header/totals]))

;; Report

(s/def :report/headers (s/nilable ::report-header))
(s/def :report/items (s/coll-of ::report-item))

;; mapping license type => frequency
(s/def :report/totals (s/map-of string? number?))

;; list of license types that make program fail
(s/def :report/fails (s/nilable (s/coll-of string?)))

(s/def ::report
  (s/keys :req-un
          [:report/headers
           :report/items
           :report/totals
           :report/fails]))

;;
;; Generators
;; https://clojure.github.io/test.check/clojure.test.check.generators.html
;;

(def non-empty-str-gen
  "Generator for strings of length between 3 and 20 chars"
  (g/fmap #(apply str %) (g/vector g/char-alpha 3 20)))

(def version-str-gen
  "Generator for PyPI version in string format"
  (g/let
   [epoch g/nat
    use-epoch g/boolean
    release-major g/nat
    release-minor g/nat
    use-release-minor g/boolean
    release-micro g/nat
    use-release-micro g/boolean
    postfix-letter
    (g/elements
     #{"dev" "pre" "preview" "a" "b" "c" "rc" "alpha" "beta" "post" "rev"})
    postfix-number g/nat
    use-postfix g/boolean
    local-first (g/not-empty g/string-alphanumeric)
    local-second (g/not-empty g/string-alphanumeric)
    use-local g/boolean]
    (str
     (if use-epoch (str epoch "!") "")
     release-major
     (if use-release-minor (str "." release-minor) "")
     (if use-release-micro (str "." release-micro) "")
     (if use-postfix (str "." postfix-letter postfix-number) "")
     (if use-local (str "+" local-first "." local-second) ""))))

(def operators #{"<" "<=" ">" ">=" "==" "~=" "!="})

(def specifier-str-gen
  "Generator for PyPI package specifier in string format"
  (g/let
   [op-first (g/elements operators)
    version-first version-str-gen
    use-second-specifier g/boolean
    op-second (g/elements operators)
    version-second version-str-gen]
    (str
     op-first
     version-first
     (if use-second-specifier (str "," op-second version-second) ""))))

(def requirement-str-gen
  "Generator for PyPI package name with specifiers in string format"
  (g/let [package non-empty-str-gen
          spec specifier-str-gen]
    (str package spec)))

(def requirements-str-gen
  "Generator for a collection of requirement strings"
  (g/vector requirement-str-gen))
