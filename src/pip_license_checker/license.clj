(ns pip-license-checker.license
  "Licenses constants"
  (:gen-class)
  (:require
   [clojure.string :as str]))


;; Lincense regex

;;
;; When discriminating between permissive and copyleft licenses here,
;; we are mostly concerned with the linking, because this is the most used
;; aspect of the dependency libraries, and distribution.
;; See more details here (be careful though, as not all the details are acurate enough):
;; https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licences
;;


(def regex-list-copyleft-network
  "Copyleft licenses that consider access over the network as distribution"
  [#"^Affero"
   #"GNU Affero General Public License"])

(def regex-list-copyleft-strong
  "Copyleft licenses with wide range of activities considered as derivation"
  [;; https://www.gnu.org/licenses/gpl-faq.html
   #"GNU General Public License"
   #"^GPL"

   #"IBM Public License"])

(def regex-list-copyleft-weak
  "Weak or partial copyleft that usually not triggered for static and/or dynamic linking"
  [#"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"^LGPL"

   #"GPL.*linking exception"

   ;; https://www.mozilla.org/en-US/MPL/2.0/FAQ/
   #"^MPL"
   #"Mozilla Public License"

   #"^EUPL"
   #"European Union Public Licence"

   #"^OSL"
   #"Open Software License"

   #"^CPL"
   #"Common Public License"

   #"Artistic"

   ;; https://cecill.info/faq.en.html
   #"^CeCILL-C"
   #"CEA CNRS Inria Logiciel Libre License"
   #"^CeCILL-2.1"])

(def regex-list-copyleft-all
  "All copyleft licenses"
  (into
   []
   (concat
    regex-list-copyleft-network
    regex-list-copyleft-strong
    regex-list-copyleft-weak)))

(def regex-list-permissive
  "Permissive licenses"
  [#"CeCILL-B Free Software License Agreement"
   #"^CeCILL-B"

   #"Academic Free License"
   #"^AFL"

   #"Apache Software License"
   #"^Apache"

   #"BSD"

   #"Historical Permission Notice and Disclaimer"
   #"^HPND"

   #"Microsoft Public License"

   #"MIT License"
   #"^MIT"

   #"ISC License"
   #"^ISCL"

   #"Python Software Foundation License"
   #"Python License"

   #"Unlicense"

   #"Universal Permissive License"
   #"UPL"

   #"W3C"

   #"Zope Public License"

   #"zlib/libpng"

   #"Public Domain"])


;; Const


(def name-error "Error")

(def type-error "Error")
(def type-copyleft-all "Copyleft")
(def type-copyleft-weak "WeakCopyleft")
(def type-copyleft-strong "StrongCopyleft")
(def type-copyleft-network "NetworkCopyleft")
(def type-permissive "Permissive")
(def type-other "Other")

(def types-copyleft
  (sorted-set type-copyleft-network
              type-copyleft-strong
              type-copyleft-weak))

(def types
  (sorted-set type-error
              type-copyleft-all
              type-copyleft-weak
              type-copyleft-strong
              type-copyleft-network
              type-permissive
              type-other))

(def invalid-type
  (format "Invalid license type. Use one of: %s"
          (str/join ", " types)))

(def data-error {:name name-error :desc type-error})


;; Helpers


(defn is-type-valid?
  "Return true if license-type string is valid, false otherwise"
  [license-type]
  (contains? types license-type))
