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

(ns pip-license-checker.license
  "Licenses constants"
  (:gen-class)
  (:require
   [clojure.string :as str]
   [pip-license-checker.data :as d]))

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

;; Eclipse Public License
;; https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.txt
;; https://www.eclipse.org/legal/epl-2.0/faq.php

;; ODC-BY, ODBl
;; https://opendatacommons.org/licenses/by/1-0/
;; https://opendatacommons.org/licenses/odbl/1-0/

;; Misc
;; https://opensource.stackexchange.com/

(def regex-ignore-case #"(?i)")

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
  [#"GNU General Public License(?!.*classpath|.*linking|.*exception)"
   #"\bGPL(?!.*classpath|.*linking|.*exception)"

   #"IBM Public License"

   #"\bRPL"
   #"Reciprocal Public License"

   #"Sleepycat License"])

(def regex-list-copyleft-weak
  "Weak or partial copyleft that usually not triggered for static and/or dynamic linking"
  [#"GNU Lesser General Public License"
   #"GNU Library or Lesser General Public License"
   #"\bLGPL"

   #"GNU General Public License.*(?:classpath|linking|exception)"
   #"\bGPL.*(?:classpath|linking|exception)"

   #"\bMPL"
   #"Mozilla Public License"

   #"\bMotosoto"

   #"Nokia Open Source License"

   #"\bNPL"
   #"Netscape Public License"

   #"\bNOSL"
   #"Netizen Open Source License"

   #"\bODbL"
   #"Open Database License"

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

   #"CC0"
   #"CC(-?|\s*)BY(-?|\s*)(?!.*SA|.*share|.*ND|.*derivative|.*NC|.*commercial)"

   #"CeCILL-B Free Software License Agreement"
   #"CeCILL-B"

   #"Eiffel Forum License"

   #"Historical Permission Notice and Disclaimer"
   #"\bHPND"

   #"MIT License"
   #"\bMIT"

   #"MirOS"

   #"Intel Open Source License"

   #"\bISC\b"
   #"\bISCL"
   #"ISC License"

   #"Ms-PL"
   #"Microsoft Public License"

   #"\bODC(-?|\s*)BY"
   #"Open Data Commons Attribution License"

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

   #"\bzlib(?!-|\/)\b"

   ;; Like zlib license above but requires that an acknowledgement be made in the "product documentation"
   #"\bzlib(.*acknowledgement|\/libpng)\b"

   #"WTFPL"
   #"Do What the Fuck You Want To Public License"])

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

(def license-error (d/->License name-error type-error nil))

;; Functions

(defn get-license-error
  "Get license object of error type"
  [ex]
  (d/->License name-error type-error ex))

(defn is-type-valid?
  "Return true if license-type string is valid, false otherwise"
  [license-type]
  (contains? types license-type))

(defn strings->pattern
  "Get regex pattern from sequence of strings"
  [patterns]
  (re-pattern
   (str regex-ignore-case
        (apply str (interpose "|" (map #(str "(?:" % ")") patterns))))))

(defn license-with-type
  "Get license type by its name"
  [name]
  (try
    (let [regex-copyleft-network (strings->pattern regex-list-copyleft-network)
          regex-copyleft-strong (strings->pattern regex-list-copyleft-strong)
          regex-copyleft-weak (strings->pattern regex-list-copyleft-weak)
          match-copyleft-network (some? (re-find regex-copyleft-network name))
          match-copyleft-strong (some? (re-find regex-copyleft-strong name))
          match-copyleft-weak (some? (re-find regex-copyleft-weak name))

          regex-permissive (strings->pattern regex-list-permissive)
          match-permissive (some? (re-find regex-permissive name))]
      (cond
        match-copyleft-network (d/->License name type-copyleft-network nil)
        match-copyleft-strong (d/->License name type-copyleft-strong nil)
        match-copyleft-weak (d/->License name type-copyleft-weak nil)
        match-permissive (d/->License name type-permissive nil)
        :else (d/->License name type-other nil)))
    (catch NullPointerException _
      license-error)))
