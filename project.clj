(defproject org.clojars.vrs/pip-license-checker "0.17.0"
  :description "Check Python PyPI package license"
  :url "https://github.com/pilosus/pip-license-checker"
  :license {:name "The MIT License (MIT)"
            :url "https://github.com/pilosus/pip-license-checker/blob/main/LICENSE"}
  :dependencies [
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "1.0.194"]
                 [clj-http "3.11.0"]
                 [cheshire "5.10.0"]
                 [org.clojure/test.check "1.1.0"]]
  :plugins [[lein-cljfmt "0.7.0"]
            [lein-cloverage "1.2.1"]]
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
