(ns pip-license-checker.version
  "Version parsing and comparing"
  (:gen-class)
  (:require
   [clojure.string :as str]))

;; Parse version

(def regex-version #"v?(?:(?:(?<epoch>[0-9]+)!)?(?<release>[0-9]+(?:\.[0-9]+)*)(?<pre>[-_\.]?(?<prel>(a|b|c|rc|alpha|beta|pre|preview))[-_\.]?(?<pren>[0-9]+)?)?(?<post>(?:-(?<postn1>[0-9]+))|(?:[-_\.]?(?<postl>post|rev|r)[-_\.]?(?<postn2>[0-9]+)?))?(?<dev>[-_\.]?(?<devl>dev)[-_\.]?(?<devn>[0-9]+)?)?)(?:\+(?<local>[a-z0-9]+(?:[-_\.][a-z0-9]+)*))?")

(defn parse-number
  "Parse number string into integer or return 0"
  [number]
  (if (not number) 0 (Integer/parseInt number)))

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
                canonical-number (parse-number number)]
            [canonical-letter canonical-number])
          (and (not letter) number)
          (let [canonical-letter "post"
                canonical-number (parse-number number)]
            [canonical-letter canonical-number])
          :else nil)]
    result))

(defn parse-local-version
  "Parse strings into vec with string parsed into ints if possible"
  [local]
  (let [lowered (if local (str/lower-case local) nil)
        splitted (if lowered (str/split lowered #"[\._-]") nil)
        parsed
        (vec (map
              #(try
                 (Integer/parseInt %)
                 (catch NumberFormatException _ %))
              splitted))]
    (if (= parsed []) nil parsed)))

(defn validate-version
  [version-map]
  (let [{:keys
         [orig
          epoch
          release
          prel pren
          postn1 postl postn2
          devl devn
          local]} version-map
        result
        {:orig orig
         :epoch (if epoch (Integer/parseInt epoch) 0)
         :release (vec (map #(Integer/parseInt %) (str/split release #"\.")))
         :pre (parse-letter-version prel pren)
         :post (parse-letter-version postl (or postn1 postn2))
         :dev (parse-letter-version devl devn)
         :local (parse-local-version local)}]
    result))

(defn parse-version
  "Return a hash-map of regex groups"
  [version-str]
  (let [matcher (re-matcher regex-version version-str)
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
           :local (.group matcher "local")}
          nil)]
    (validate-version version-map)))


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
          (and (not pre) (not post) dev) (Double/NEGATIVE_INFINITY)
          (not pre) (Double/POSITIVE_INFINITY)
          :else pre)
        post
        (cond
          (not post) (Double/NEGATIVE_INFINITY)
          :else post)
        dev
        (cond
          (not dev) (Double/POSITIVE_INFINITY)
          :else dev)
        local
        (cond
          (not local) [[(Double/NEGATIVE_INFINITY) ""]]
          :else
          (vec (map #(if (integer? %)
                       [% ""]
                       [(Double/NEGATIVE_INFINITY) %]) local)))]
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

;; Specifiers for parsed versions

(defn version-ok?
  "Return true if a parsed version satisfies each specifier
  Specifiers is a collection of vec [specifier-op specifier-version]"
  [specifiers version]
  (every?
   true?
   (map
    (fn [[spec-op spec-version]] (spec-op version spec-version))
    specifiers)))

(defn filter-versions
  "Return lazy seq of parsed versions that satisfy specifiers"
  [specifiers versions]
  (filter #(version-ok? specifiers %) versions))

(defn sort-versions
  "Sort a vector of parsed versions.
  Ascending sort order is used by default."
  [versions & {:keys [order] :or {order :asc}}]
  (let [comparator-fn
        (if (= order :asc)
          #(compare-version %1 %2)
          #(compare-version %2 %1))]
    (sort comparator-fn versions)))
