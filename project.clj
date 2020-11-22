(defproject pip-license-checker "0.1.0-SNAPSHOT"
  :description "Check Python PyPI package license"
  :url "https://github.com/pilosus/pip-license-checker"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.1"]
                 [clj-http "3.11.0"]
                 [cheshire "5.10.0"]]
  :main pip-license-checker.core
  :aot [pip-license-checker.core]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
