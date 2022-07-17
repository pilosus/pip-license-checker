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

(ns pip-license-checker.spec
  "Specs"
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test.check.generators :as g]))

;;
;; Macros
;;
;; (require '[pip-license-checker.spec :as sp])
;; (def ->int (sp/with-conformer [val] (Integer/parseInt val)))
;; (s/def ::->int ->int)
;; (s/conform ::->int "123")

(defmacro with-conformer
  [[bind] & body]
  `(s/conformer
    (fn [~bind]
      (try
        ~@body
        (catch Exception e#
          ::s/invalid)))))

;;
;; Specs
;;
;; (require '[pip-license-checker.spec :as sp])
;; (s/valid? ::sp/requirement "abc")

;; requirement strings

(s/def ::requirement string?)
(s/def ::requirements (s/coll-of ::requirement))

(defn regex?
  [object]
  (= (type object) java.util.regex.Pattern))

(s/def ::opt-pattern (s/nilable regex?))

;; Versions

(s/def ::version-str string?)
(s/def ::matched-version-part (s/nilable string?))
(s/def ::non-negative-int (s/and int? (fn [n] (>= n 0))))
(s/def ::version-orig string?)
(s/def ::version-epoch ::non-negative-int)
(s/def ::version-release (s/coll-of int?))

(s/def ::opt-version-letter
  (s/nilable (s/tuple string? int?)))

(s/def ::opt-version-local
  (s/nilable (s/* (s/alt :letter string? :number int?))))

(s/def :version/orig ::version-orig)
(s/def :version/epoch ::version-epoch)
(s/def :version/release ::version-release)
(s/def :version/pre ::opt-version-letter)
(s/def :version/post ::opt-version-letter)
(s/def :version/dev ::opt-version-letter)
(s/def :version/local ::opt-version-local)

(s/def ::version
  (s/keys :req-un
          [:version/orig
           :version/epoch
           :version/release
           :version/pre
           :version/post
           :version/dev
           :version/local]))

(s/def ::versions
  (s/nilable (s/coll-of ::version)))

;; Specifiers

(s/def ::specifier-str string?)
(s/def ::op
  (s/fspec :args (s/cat :a ::version :b ::version)
           :ret boolean?))

(s/def ::specifier
  (s/nilable (s/tuple ::op ::version)))

(s/def ::specifiers
  (s/nilable (s/coll-of ::specifier)))

;; License

(s/def ::requirement-map
  (s/cat :name ::requirement :specifiers ::specifiers))

(s/def ::license-str string?)

;; HTTP response

(s/def ::requirement-response
  (s/cat
   :ok? boolean?
   :requirement (s/cat
                 :name ::requirement
                 :version ::version-str)
   :response (s/?
              (s/map-of string? string?))))

(s/def ::requirement-response-data
  (s/cat
   :ok? boolean?
   :requirement (s/cat
                 :name ::requirement
                 :version ::version-str)
   :data (s/?
          (s/map-of string? any?))))

(s/def ::requirement-response-license
  (s/cat
   :ok? boolean?
   :requirement (s/cat
                 :name ::requirement
                 :version ::version-str)
   :license (s/cat
             :name string?
             :type string?)))


;; CLI


(s/def ::requirements-cli-arg (s/nilable (s/coll-of string?)))
(s/def ::packages-cli-arg (s/nilable (s/coll-of string?)))
(s/def ::options-cli-arg (s/nilable (s/map-of string? string?)))
(s/def ::options-fail (s/nilable (s/coll-of set?)))


;; Core


(s/def ::license-type-totals (s/map-of string? int?))


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
