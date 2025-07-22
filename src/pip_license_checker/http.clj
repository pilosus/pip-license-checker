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

(ns pip-license-checker.http
  (:gen-class)
  (:require
   [clj-http.client :as http]
   [indole.core :refer [can-charge?!]]))

(defn make-delay
  []
  (Thread/sleep 200))

(defn request-get
  "Make GET request"
  [url settings rate-limiter]
  (if (can-charge?! rate-limiter)
    (http/get url settings)
    (do
      (make-delay)
      (recur url settings rate-limiter))))
