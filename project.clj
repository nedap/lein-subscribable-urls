;; Please don't bump the library version by hand - use ci.release-workflow instead.
(defproject lein-subscribable-urls "unreleased"
  ;; Please keep the dependencies sorted a-z.
  :dependencies []

  :eval-in-leiningen ~(not (System/getenv "skip_eval_in_leiningen"))

  :description "lein-subscribable-urls"

  :url "https://github.com/nedap/lein-subscribable-urls"

  :min-lein-version "2.0.0"

  :license {:name "EPL-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :signing {:gpg-key "releases-staffingsolutions@nedap.com"}

  :repositories {"releases" {:url      "https://nedap.jfrog.io/nedap/staffing-solutions/"
                             :username :env/artifactory_user
                             :password :env/artifactory_pass}}

  :repository-auth {#"https://nedap.jfrog\.io/nedap/staffing-solutions/"
                    {:username :env/artifactory_user
                     :password :env/artifactory_pass}}

  :deploy-repositories {"clojars" {:url      "https://clojars.org/repo"
                                   :username :env/clojars_user
                                   :password :env/clojars_pass}}
  :target-path "target/%s"

  :test-paths ["src" "test"]

  :monkeypatch-clojure-test false

  :plugins [[lein-pprint "1.1.2"]]

  :profiles {:check {:global-vars {*unchecked-math* :warn-on-boxed
                                   ;; avoid warnings that cannot affect production:
                                   *assert*         false}}

             :test  {:dependencies [[com.nedap.staffing-solutions/utils.test "1.6.1"]]
                     :jvm-opts     ["-Dclojure.core.async.go-checking=true"
                                    "-Duser.language=en-US"]}

             :ci    {:pedantic?    :abort
                     :jvm-opts     ["-Dclojure.main.report=stderr"]
                     :global-vars  {*assert* true} ;; `ci.release-workflow` relies on runtime assertions
                     :dependencies [[com.nedap.staffing-solutions/ci.release-workflow "1.6.0"]]}})
