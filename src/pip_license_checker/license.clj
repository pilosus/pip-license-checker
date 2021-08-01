(ns pip-license-checker.license
  "Licenses constants"
  (:gen-class)
  (:require
   [clojure.string :as str]))


;; Lincense regex

;;
;; When discriminating between permissive and copyleft licenses here,
;; we are mostly concerned with the dynamic *linking*, because this is
;; the most used aspect of the dependency libraries for a Python program.
;; See more details here (be careful though, as not all the details
;; are acurate enough):
;; https://en.wikipedia.org/wiki/Comparison_of_free_and_open-source_software_licences
;;
;; Another useful links to learn about licenses
;;
;; GNU licenses (GPL, LGPL, AGPL)
;; https://www.gnu.org/licenses/
;; https://www.gnu.org/licenses/gpl-3.0.txt
;; https://www.gnu.org/licenses/gpl-faq.html

;; Free Software Movement license list
;; https://www.gnu.org/licenses/license-list.html

;; OSI license list
;; https://opensource.org/licenses

;; European Union Public Licence (EUPL)
;; https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
;; https://joinup.ec.europa.eu/collection/eupl/matrix-eupl-compatible-open-source-licences#section-3

;; Mozilla Public License (MPL)
;; https://www.mozilla.org/en-US/MPL/
;; https://www.mozilla.org/en-US/MPL/2.0/FAQ/

;; CeCILL
;; https://cecill.info/licences.en.html
;; https://cecill.info/faq.en.html

;; Misc
;; https://tldrlegal.com/
;; https://opensource.stackexchange.com/


(def regex-list-copyleft-network
  "Copyleft licenses that consider access over the network as distribution"
  [#"\bAffero"
   #"\bAGPL"
   #"GNU Affero General Public License"

   #"\bOSL"
   #"Open Software License"

   #"\bRPSL"
   #"RealNetworks Public Source License"

   #"\bWatcom"
   #"Sybase Open Watcom Public License"])

(def regex-list-copyleft-strong
  "Copyleft licenses with wide range of activities considered as derivation"
  [#"GNU General Public License"
   #"\bGPL"

   #"IBM Public License"

   #"\bRPL"
   #"Reciprocal Public License"

   #"Sleepycat License"])

(def regex-list-copyleft-weak
  "Weak or partial copyleft that usually not triggered for static and/or dynamic linking"
  [#"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"\bLGPL"

   #"GPL.*linking exception"

   #"\bMPL"
   #"Mozilla Public License"

   #"Motosoto"

   #"Nokia Open Source License"

   #"\bNPL"
   #"Netscape Public License"

   #"\bNOSL"
   #"Netizen Open Source License"

   #"BitTorrent Open Source License"

   #"\bSPL"
   #"Sun Public License"

   #"\bEPL"
   #"Eclipse Public License"

   ;; Linking as producing a derivative work is not confirmed by case law yet,
   ;; but probably contradicts the EU laws, that's why weak copyleft
   ;; If proved otherwise, may be moved to a strong copyleft list
   ;; See https://joinup.ec.europa.eu/collection/eupl/matrix-eupl-compatible-open-source-licences#section-3
   #"\bEUPL"
   #"European Union Public Licence"

   #"Eurosym License"

   #"\bCPL"
   #"Common Public License"

   #"\bCDDL"
   #"Common Development and Distribution License"

   #"CeCILL-C"
   #"CEA CNRS Inria Logiciel Libre License"
   #"CeCILL-2.1"

   #"\bAPSL"
   #"Apple Public Source License"

   #"Ms-RL"
   #"Microsoft Reciprocal License"

   #"\bOFL"
   #"SIL Open Font License"])

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
  [#"\bAFL"
   #"Academic Free License"

   #"\bApache"
   #"Apache Software License"

   #"\bAAL"
   #"Attribution Assurance License"

   #"Artistic"

   #"BSD"

   #"CeCILL-B Free Software License Agreement"
   #"CeCILL-B"

   #"Eiffel Forum License"

   #"Historical Permission Notice and Disclaimer"
   #"\bHPND"

   #"MIT License"
   #"\bMIT"

   #"MirOS"

   #"Intel Open Source License"

   #"\bISCL"
   #"ISC License"

   #"Ms-PL"
   #"Microsoft Public License"

   #"PostgreSQL License"

   #"Python Software Foundation License"
   #"Python License"

   #"Public Domain"

   #"\bQPL"
   #"Q Public License"

   #"Repoze Public License"

   #"Unlicense"

   #"\bUPL"
   #"Universal Permissive License"

   #"\bUIUC"
   #"University of Illinois"
   #"NCSA Open Source License"
   #"University of Illinois/NCSA Open Source License"

   #"Vovida Software License"

   #"W3C"

   #"X.Net License"

   #"Zope Public License"

   #"zlib/libpng"])


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
