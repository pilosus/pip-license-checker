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

(ns pip-license-checker.version
  "Version parsing and comparing"
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [pip-license-checker.spec :as sp]))

;; const

(def regex-number #"^\d+$")
(def regex-split-comma #",")
(def regex-specifier #"(?<op>(===|==|~=|!=|>=|<=|<|>))(?<version>(.*))")

(def regex-version #"v?(?:(?:(?<epoch>[0-9]+)!)?(?<release>[0-9]+(?:\.[0-9]+)*)(?<pre>[-_\.]?(?<prel>(a|b|c|rc|alpha|beta|pre|preview))[-_\.]?(?<pren>[0-9]+)?)?(?<post>(?:-(?<postn1>[0-9]+))|(?:[-_\.]?(?<postl>post|rev|r)[-_\.]?(?<postn2>[0-9]+)?))?(?<dev>[-_\.]?(?<devl>dev)[-_\.]?(?<devn>[0-9]+)?)?)(?:\+(?<local>[a-z0-9]+(?:[-_\.][a-z0-9]+)*))?")

(def extension-sdist ".tar.gz")
(def extension-wheel ".whl")
(def regex-extensions #"\.(tar\.gz|whl)")

;; filename parsing

(defn normalize-project
  "Normalize project name"
  [project]
  (when project
    (-> project
        (str/replace #"[-_.]+" "-")
        str/lower-case)))

(defn get-project-regex
  [project]
  (let [normalized (normalize-project project)
        replaced (str/replace normalized #"-" "[-_.]+")
        case-insensitive (str "(?i)^" replaced "-")]
    (re-pattern case-insensitive)))

(defn trim-filename-version
  "Remove project name and file extension from distribution filename

  Expected output format depends on the distribution type:
  - sdist:
  `{version}
  - wheel:
  `{version}(-{build tag})?-{python tag}-{abi tag}-{platform tag}`"
  [filename project]
  (let [regex-project (get-project-regex project)]
    (-> filename
        str/lower-case
        (str/replace regex-project "")
        (str/replace regex-extensions ""))))

(defmulti get-dist-version
  "Get Python distribution version based on the format

  The following formats are supported
  - Source distibution (sdist)
  https://packaging.python.org/en/latest/specifications/source-distribution-format/
  sdist version parsing relies on packages to comply with PEP-625
  https://peps.python.org/pep-0625/

  - Binary distribution (wheel)
  https://packaging.python.org/en/latest/specifications/binary-distribution-format/
  wheel version parsing relies on packages to comply with PEP-427
  https://peps.python.org/pep-0427/

  Not supported formats:
  - Built distribution (bdist) - legacy
  https://docs.python.org/3/distutils/builtdist.html"
  (fn [filename _]
    (cond
      (str/ends-with? filename extension-wheel) :wheel
      (str/ends-with? filename extension-sdist) :sdist)))

(defmethod get-dist-version :sdist [filename project]
  (trim-filename-version filename project))

(defn- get-version-tag
  "Get version tag from the filename for eggs and wheels filenames"
  [filename project]
  (let [tags (-> filename
                 (trim-filename-version project)
                 (str/split #"-"))]
    (first tags)))

(defmethod get-dist-version :wheel [filename project]
  (get-version-tag filename project))

(defmethod get-dist-version :default [filename project]
  (let [version (get-version-tag filename project)]
    (first (re-find regex-version version))))

;; version parsing

(defn parse-int
  "Parse number string, assume BigInteger"
  [number]
  (BigInteger. number))

(defn parse-int-or-get-0
  "Parse number string into integer or return 0"
  [number]
  (try (parse-int number) (catch Exception _ 0)))

(s/fdef parse-letter-version
  :args (s/cat :letter ::sp/matched-version-part
               :number ::sp/matched-version-part)
  :ret ::sp/opt-version-letter)

(defn parse-letter-version
  "Parse letter part of version"
  [letter number]
  (let [result
        (cond
          letter
          (let [sanitized-letter (str/lower-case letter)
                canonical-letter
                (cond
                  (= sanitized-letter "alpha") "a"
                  (= sanitized-letter "beta") "b"
                  (contains? #{"c" "pre" "preview"} sanitized-letter) "rc"
                  (contains? #{"rev" "r"} sanitized-letter) "post"
                  :else sanitized-letter)
                canonical-number (parse-int-or-get-0 number)]
            [canonical-letter canonical-number])
          (and (not letter) number)
          (let [canonical-letter "post"
                canonical-number (parse-int-or-get-0 number)]
            [canonical-letter canonical-number])
          :else nil)]
    result))

(s/fdef parse-local-version
  :args (s/cat :local ::sp/matched-version-part)
  :ret ::sp/opt-version-local)

(defn parse-local-version
  "Parse strings into vec with string parsed into ints if possible"
  [local]
  (let [lowered (if local (str/lower-case local) nil)
        splitted (if lowered (str/split lowered #"[\._-]") nil)
        parsed
        (vec (map
              #(try
                 (parse-int %)
                 (catch NumberFormatException _ %))
              splitted))]
    (if (= parsed []) nil parsed)))

(defn validate-version
  [version-map]
  (if version-map
    (let [{:keys
           [orig
            epoch
            release
            prel pren
            postn1 postl postn2
            devl devn
            local
            meta]} version-map
          result
          {:orig orig
           :epoch (if epoch (parse-int epoch) 0)
           :release (vec (map #(parse-int %) (str/split release #"\.")))
           :pre (parse-letter-version prel pren)
           :post (parse-letter-version postl (or postn1 postn2))
           :dev (parse-letter-version devl devn)
           :local (parse-local-version local)
           :meta meta}]
      result)
    nil))

(s/fdef parse-version
  ;; address mutli-arity with s/alt
  :args (s/alt :unary (s/cat :version-str ::sp/version-str)
               :binary (s/cat :version-str ::sp/version-str
                              :meta ::sp/version-meta))
  :ret ::sp/version)

(defn parse-version
  "Return a hash-map of regex groups"
  ([version-str]
   (parse-version version-str nil))
  ([version-str meta]
   (let [sanitized-version (str/lower-case version-str)
         matcher (re-matcher regex-version sanitized-version)
         version-map
         (if (.matches matcher)
           {:orig version-str
            :epoch (.group matcher "epoch")
            :release (.group matcher "release")
            :pre (.group matcher "pre")
            :prel (.group matcher "prel")
            :pren (.group matcher "pren")
            :post (.group matcher "post")
            :postn1 (.group matcher "postn1")
            :postl (.group matcher "postl")
            :postn2 (.group matcher "postn2")
            :dev (.group matcher "dev")
            :devl (.group matcher "devl")
            :devn (.group matcher "devn")
            :local (.group matcher "local")
            :meta meta}
           nil)]
     (validate-version version-map))))

;; Comparison of parsed versions
;; https://clojuredocs.org/clojure.core/compare
;; https://clojure.org/guides/comparators

(defn truncate-release
  "Return release vector with trailing zero parts dropped"
  [release]
  (let [release-truncated
        (vec (reverse (drop-while #(= % 0) (reverse release))))]
    release-truncated))

(defn get-comparable-version
  "Get parsed version map formatted to be comparable
  See more details in pypa/packaging:
  https://github.com/pypa/packaging/blob/20.8/packaging/version.py#L505"
  [version-map]
  (let [{:keys [orig epoch release pre post dev local]} version-map
        release (truncate-release release)
        pre
        (cond
          (and (not pre) (not post) dev) Double/NEGATIVE_INFINITY
          (not pre) Double/POSITIVE_INFINITY
          :else pre)
        post
        (cond
          (not post) Double/NEGATIVE_INFINITY
          :else post)
        dev
        (cond
          (not dev) Double/POSITIVE_INFINITY
          :else dev)
        local
        (cond
          (not local) [[Double/NEGATIVE_INFINITY ""]]
          :else
          (vec (map #(if (integer? %)
                       [% ""]
                       [Double/NEGATIVE_INFINITY %]) local)))]
    {:orig orig
     :epoch epoch
     :release release
     :pre pre
     :post post
     :dev dev
     :local local}))

(defn pad-vector
  "Append padding values to a given vector to make it of the specified len
  Used to compare vectors of numbers"
  [vec-val len pad-val]
  (let [size (count vec-val)
        left (- len size)
        prepend (vec (repeat left pad-val))]
    (vec (concat vec-val prepend))))

(defn compare-letter-version
  "Compare vectors of [name version] shape with possible fallbacks to +/- Inf
  NB! Comparator will break if assumed shape of vectors is violated"
  [a b]
  (cond
    (or
     (and (vector? a) (vector? b))
     (and (number? a) (number? b))) (compare a b)
    (and (number? a) (not (number? b))) (compare a 0)
    (and (not (number? a)) (number? b)) (compare 0 b)
    :else
    (throw
     (ex-info
      (format "Cannot compare letter-version vectors")
      {:a a :b b}))))

(defn compare-version
  "Compare version maps"
  [a b]
  (let [;; compare epochs
        a (get-comparable-version a)
        b (get-comparable-version b)
        c-epoch (compare (:epoch a) (:epoch b))
        ;; compare releases
        release-a (:release a)
        release-b (:release b)
        max-release-len (max (count release-a) (count release-b))
        release-a-padded (pad-vector release-a max-release-len 0)
        release-b-padded (pad-vector release-b max-release-len 0)
        c-release (compare release-a-padded release-b-padded)
        ;; compare pre, post, dev parts
        c-pre (compare-letter-version (:pre a) (:pre b))
        c-post (compare-letter-version (:post a) (:post b))
        c-dev (compare-letter-version (:dev a) (:dev b))
        ;; compare local
        local-a (:local a)
        local-b (:local b)
        max-local-len (max (count local-a) (count local-b))
        local-a-padded (pad-vector local-a max-local-len [0 ""])
        local-b-padded (pad-vector local-b max-local-len [0 ""])
        c-local (compare local-a-padded local-b-padded)
        ;; get all comparators
        c-all [c-epoch c-release c-pre c-post c-dev c-local]
        result (some #(if (not= % 0) % nil) c-all)]
    (or result 0)))

(defn eq
  "Return true if versions a and b are equal"
  [a b]
  (let [comparator (compare-version a b)]
    (= comparator 0)))

(defn neq
  "Return true if versions a and b are not equal"
  [a b]
  (let [comparator (compare-version a b)]
    (not= comparator 0)))

(defn lt
  "Return true if version a less than b"
  [a b]
  (let [comparator (compare-version a b)]
    (neg? comparator)))

(defn le
  "Return true if version a less than or equal to b"
  [a b]
  (let [comparator (compare-version a b)]
    (<= comparator 0)))

(defn gt
  "Return true if version a greater than b"
  [a b]
  (let [comparator (compare-version a b)]
    (pos? comparator)))

(defn ge
  "Return true if version a greater than or equal to b"
  [a b]
  (let [comparator (compare-version a b)]
    (>= comparator 0)))

(defn compatible
  "Return true if version a is compatible with b
  Compatible releases have an equivalent combination of >= and ==.
  That is that ~=2.2 is equivalent to >=2.2,==2.*.
  See more:
  https://www.python.org/dev/peps/pep-0440/#compatible-release"
  [a b]
  (let [b-release-trunc (vec (take (- (count (:release b)) 1) (:release b)))
        a-release-trunc (vec (take (count b-release-trunc) (:release a)))]
    (and (ge a b) (= a-release-trunc b-release-trunc))))

(defn arbitrary-eq
  "Return true if string representation of version a equal to b
  See more:
  https://www.python.org/dev/peps/pep-0440/#arbitrary-equality"
  [a b]
  (let [a-str (:orig a)
        b-str (:orig b)]
    (= a-str b-str)))

;; Parse specifier string

(defn get-comparison-op
  "Get comparison function for operator string"
  [op]
  (case op
    "===" arbitrary-eq
    "==" eq
    "~=" compatible
    "!=" neq
    "<=" le
    ">=" ge
    "<" lt
    ">" gt))

(s/fdef parse-specifier
  :args (s/cat :specifier-str ::sp/specifier-str)
  :ret ::sp/specifier)

(defn parse-specifier
  "Parse single specifier string into a vec of operator function and version map.
  E.g. '>=1.2.3 parsed into [parsed-op parsed-version]"
  [specifier-str]
  (let [matcher (re-matcher regex-specifier specifier-str)
        specifier-pair
        (if (.matches matcher)
          [(get-comparison-op (.group matcher "op"))
           (parse-version (.group matcher "version"))]
          nil)]
    specifier-pair))

(s/fdef parse-specifiers
  :args (s/cat :specifiers-str string?)
  :ret ::sp/specifiers)

(defn parse-specifiers
  "Parse a string of specifiers into a vec of parsed specifier vecs/
  E.g. '>=1.2.3,<2' parsed into [[>=' 1.2.3'] [<' 2']]"
  [specifiers-str]
  (let [specifiers-vec (str/split specifiers-str regex-split-comma)
        result (vec (map parse-specifier specifiers-vec))]
    result))

;; Versions filtering helpers

(defn same-release-with-post-or-local?
  "Check if version to be excluded from >V comparison as per
  https://www.python.org/dev/peps/pep-0440/#exclusive-ordered-comparison"
  [a b]
  (let [a-post? (not (nil? (:post a)))
        a-local? (not (nil? (:local a)))
        a-release (:release a)

        b-post? (not (nil? (:post b)))
        b-local? (not (nil? (:local b)))
        b-release (:release b)

        max-release-len
        (max (count a-release) (count b-release))
        a-release* (pad-vector a-release max-release-len 0)
        b-release* (pad-vector b-release max-release-len 0)

        result
        (and
         (= a-release* b-release*)
         (or a-post? a-local?)
         (not (or b-post? b-local?)))]
    result))

(defn same-release-with-pre-or-local?
  "Check if version to be excluded from <V comparison as per
  https://www.python.org/dev/peps/pep-0440/#exclusive-ordered-comparison"
  [a b]
  (let [a-pre? (not (nil? (:pre a)))
        a-local? (not (nil? (:local a)))
        a-release (:release a)

        b-pre? (not (nil? (:pre b)))
        b-local? (not (nil? (:local b)))
        b-release (:release b)

        max-release-len
        (max (count b-release) (count a-release))
        a-release* (pad-vector a-release max-release-len 0)
        b-release* (pad-vector b-release max-release-len 0)

        result
        (and
         (= a-release* b-release*)
         (or a-pre? a-local?)
         (not (or b-pre? b-local?)))]
    result))

;; Check version against specifiers

(s/fdef version-ok?
  :args (s/cat :specifiers ::sp/specifiers :version ::sp/version)
  :ret boolean?)

(defn version-ok?
  "Return true if a parsed version satisfies each specifier
  Specifiers is a collection of vec [specifier-op specifier-version]"
  [specifiers version]
  (every?
   true?
   (map
    (fn [[spec-op spec-version]]
      (and (spec-op version spec-version)
           (not
            (and (= spec-op gt)
                 (same-release-with-post-or-local? version spec-version)))
           (not
            (and (= spec-op lt)
                 (same-release-with-pre-or-local? version spec-version)))))
    specifiers)))

(s/fdef version-stable?
  :args (s/cat :version ::sp/version)
  :ret boolean?)

(defn version-stable?
  "Return true if version is neither pre-release or development version"
  [version]
  (let [version-not-pre? (nil? (:pre version))
        version-not-dev? (nil? (:dev version))
        result (and version-not-pre? version-not-dev?)]
    result))

(defn- contains-eq-specifier?
  "Return true if the specifiers vector contains `==` or `===`"
  [specifiers]
  (some #(contains? #{eq arbitrary-eq} (first %)) specifiers))

(defn remove-yanked-versions
  "Remove yanked versions for non-exact specifiers"
  [specifiers versions]
  (if (contains-eq-specifier? specifiers)
    versions
    (remove #(= (get-in % [:meta :yanked]) true) versions)))

(defn filter-versions
  "Return lazy seq of parsed versions that satisfy specifiers"
  [specifiers versions & {:keys [pre] :or {pre true}}]
  (let [pre-release-included?
        (or pre (contains-eq-specifier? specifiers))
        versions'
        (->>
         versions
         (filter #(version-ok? specifiers %))
         (remove-yanked-versions specifiers))]
    (if pre-release-included?
      versions'
      (filter version-stable? versions'))))

(s/fdef sort-versions
  :args (s/cat :versions ::sp/versions
               :order (s/? keyword?)
               :value (s/? keyword?))
  :ret ::sp/versions)

(defn sort-versions
  "Sort a vector of parsed versions.
  Ascending sort order is used by default."
  [versions & {:keys [order] :or {order :asc}}]
  (let [comparator-fn
        (if (= order :asc)
          #(compare-version %1 %2)
          #(compare-version %2 %1))]
    (sort comparator-fn versions)))

(s/fdef get-version
  :args (s/cat :specifiers ::sp/specifiers
               :versions ::sp/versions
               :pre (s/nilable keyword?)
               :value (s/nilable boolean?))
  :ret ::sp/version-str)

(defn get-version
  "Get the most recent version from given versions that satisfies specifiers"
  [specifiers versions & {:keys [pre] :or {pre true}}]
  (let [versions-ok (filter-versions specifiers versions :pre pre)
        versions-sorted (sort-versions versions-ok)
        version-latest (last versions-sorted)
        version (:orig version-latest)]
    version))
