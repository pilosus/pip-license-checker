(ns pip-license-checker.resolve.version
  "Version parsing and comparing"
  (:gen-class)
  (:require
   [clojure.string :as str]))

;; Version parsing

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
         [epoch
          release
          prel pren
          postn1 postl postn2
          devl devn
          local]} version-map
        result
        {:epoch (if epoch (Integer/parseInt epoch) 0)
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
          {:epoch (.group matcher "epoch")
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


;; Comparison
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
  (let [{:keys [epoch release pre post dev local]} version-map
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
          (not local) [["" (Double/NEGATIVE_INFINITY)]]
          :else
          (vec (map #(if (integer? %)
                       ["" %]
                       [% (Double/NEGATIVE_INFINITY)]) local)))]
    {:epoch epoch
     :release release
     :pre pre
     :post post
     :dev dev
     :local local}))

(defn compare-version
  "Compare version maps"
  [a b]
  (let [a (get-comparable-version a)
        b (get-comparable-version b)]
    (compare [(:epoch a) (:release a) (:pre a) (:post a) (:dev a) (:local a)]
             [(:epoch b) (:release b) (:pre b) (:post b) (:dev b) (:local b)])))

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

;; FIXME
(defn compatible
  "Return true if version a is compatible with b
   Compatible releases have an equivalent combination of >= and ==.
   That is that ~=2.2 is equivalent to >=2.2,==2.*."
  [a b]
  (let [a-release-trunc (vec (take (- (count (:release a)) 1) (:release a)))
        b-release-trunc (vec (take (- (count (:release b)) 1) (:release b)))
        a-trunc (update a :release (constantly a-release-trunc))
        b-trunc (update b :release (constantly b-release-trunc))]
    (println a-trunc)
    (println b-trunc)
    (and (ge a b) (eq a-trunc b-trunc))))
