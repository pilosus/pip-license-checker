(defproject org.pilosus/pip-license-checker "0.49.0"
  :description "License compliance tool to identify dependencies license names and types: permissive, copyleft, proprietory, etc."
  :url "https://github.com/pilosus/pip-license-checker"
  :license {:name "Eclipse Public License 2.0 OR GNU GPL v2+ with Classpath exception"
            :url "https://github.com/pilosus/pip-license-checker/blob/main/LICENSE"}
  :dependencies [
                 [cheshire "6.0.0"]
                 [clj-http "3.12.3"]
                 [com.github.bdesham/clj-plist "0.10.0"]
                 [indole "1.0.0"]
                 [org.clojure/clojure "1.12.1"]
                 [org.clojure/data.csv "1.1.0"]
                 [org.clojure/test.check "1.1.1"]
                 [org.clojure/tools.cli "1.1.230"]]
  :plugins [[lein-cljfmt "0.9.2"]
            [lein-cloverage "1.2.4"]
            [lein-licenses "0.2.2"]
            [lein-ancient "1.0.0-RC3"]
            [com.github.clj-kondo/lein-clj-kondo "2025.06.05-2"]]
  :main pip-license-checker.core
  :aot [pip-license-checker.core]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :test {:cloverage {:fail-threshold 80
                                :exclude-call [clojure.spec.alpha/assert
                                               clojure.spec.alpha/def]}}}
  :test-selectors {:integration :integration
                   :cli :cli}
  :cljfmt {:sort-ns-references? true}
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :sign-releases false
                              :username :env/clojars_username
                              :password :env/clojars_password}]]
  )
