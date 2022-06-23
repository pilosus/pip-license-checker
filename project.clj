(defproject org.clojars.vrs/pip-license-checker "0.32.0"
  :description "License compliance tool to identify dependencies license names and types: permissive, copyleft, proprietory, etc."
  :url "https://github.com/pilosus/pip-license-checker"
  :license {:name "Eclipse Public License 2.0 OR GNU GPL v2+ with Classpath exception"
            :url "https://github.com/pilosus/pip-license-checker/blob/main/LICENSE"}
  :dependencies [
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "1.0.1"]
                 [org.clojure/tools.cli "1.0.206"]
                 [clj-http "3.12.3"]
                 [cheshire "5.11.0"]
                 [org.clojure/test.check "1.1.1"]
                 [org.clojars.vrs/cocoapods-acknowledgements-licenses "0.1.0"]
                 [org.clojars.vrs/gradle-licenses "0.2.0"]]
  :plugins [[lein-cljfmt "0.7.0"]
            [lein-cloverage "1.2.1"]
            [lein-licenses "0.2.2"]
            [lein-ancient "1.0.0-RC3"]]
  :main pip-license-checker.core
  :aot [pip-license-checker.core]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :test-selectors {:integration :integration
                   :cli :cli}
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :sign-releases false
                              :username :env/clojars_username
                              :password :env/clojars_password}]]
  )
