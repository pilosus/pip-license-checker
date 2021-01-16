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
;; (s/conform ::-> "123")

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

;; versions

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

;; specifiers
(s/def ::specifier-str string?)
(s/def ::op
  (s/fspec :args (s/cat :a ::version :b ::version)
           :ret boolean?))

(s/def ::specifier
  (s/nilable (s/tuple ::op ::version)))

(s/def ::specifiers
  (s/nilable (s/coll-of ::specifier)))


;;
;; Generators
;; https://clojure.github.io/test.check/clojure.test.check.generators.html
;;


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