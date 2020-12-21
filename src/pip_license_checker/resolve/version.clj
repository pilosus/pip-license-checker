(ns pip-license-checker.resolve.version
  "Version parsing and comparing"
  (:gen-class)
  (:require
   [clojure.string :as str]))

(def regex-version #"v?(?:(?:(?<epoch>[0-9]+)!)?(?<release>[0-9]+(?:\.[0-9]+)*)(?<pre>[-_\.]?(?<prel>(a|b|c|rc|alpha|beta|pre|preview))[-_\.]?(?<pren>[0-9]+)?)?(?<post>(?:-(?<postn1>[0-9]+))|(?:[-_\.]?(?<postl>post|rev|r)[-_\.]?(?<postn2>[0-9]+)?))?(?<dev>[-_\.]?(?<devl>dev)[-_\.]?(?<devn>[0-9]+)?)?)(?:\+(?<local>[a-z0-9]+(?:[-_\.][a-z0-9]+)*))?")

(defn parse-letter-version
  "Parse letter part of version"
  [letter number]
  (let [letter
        (cond
          (and number (not letter)) "post"
          (not letter) nil
          :else (str/lower-case letter))
        number (if (not number) 0 (Integer/parseInt number))
        validated-letter
        (cond
          (= letter "alpha") "a"
          (= letter "beta") "b"
          (contains? #{"c" "pre" "preview"} letter) "rc"
          (contains? #{"rev" "r"} letter) "post"
          :else letter)]
    {:letter validated-letter :number number}))

(defn parse-local-version
  "Parse strings like abc.1.twelve into [\"abc\" 1 \"twelve\"]"
  [local]
  (let [lowered (if local (str/lower-case local) nil)
        splitted (if lowered (str/split lowered #"[\._-]") nil)
        parsed
        (vec (map
              #(try
                 (Integer/parseInt %)
                 (catch NumberFormatException _ %))
              splitted))]
    parsed))

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


;; TODO
;; https://clojuredocs.org/clojure.core/compare
;; https://clojure.org/guides/comparators


(defn canonicalize-release
  "Return"
  [release]
  (let [release-vec (str/split release #"\.")]
    (reverse (drop-while #(= % 0) (reverse (map #(Integer/parseInt %) release-vec))))))

;; TODO
(defn compare-version
  "Compare version hash-maps"
  [a b]
  (let [{release-a :release} a
        {release-b :release} b]
    (constantly 1)))
