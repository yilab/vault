(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressable data store."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}

  :dependencies
  [[byte-streams "0.1.13"]
   [clj-time "0.8.0"]
   [com.stuartsierra/component "0.2.2"]
   [com.taoensso/timbre "3.3.1"]
   [environ "1.0.0"]
   [mvxcvi/clj-pgp "0.5.4"]
   [mvxcvi/puget "0.6.4"]
   [org.clojure/clojure "1.6.0"]
   [org.clojure/data.codec "0.1.0"]
   [prismatic/schema "0.3.0"]]

  :hiera
  {:path "target/doc/ns-hiera.png"
   :vertical? false
   :cluster-depth 2
   :ignore-ns #{user clojure byte-streams clj-time vault.search vault.tool}}

  :profiles
  {:coverage
   {:plugins
    [[lein-cloverage "1.0.2"]]}

   :dev
   {:source-paths ["dev/src"]

    :plugins
    [[codox "0.8.10"]
     [lein-marginalia "0.8.0"]]
    :dependencies
    [[org.clojure/tools.namespace "0.2.7"]]

    :codox
    {:defaults {:doc/format :markdown}
     :exclude #{user}
     :output-dir "target/doc/codox"
     :src-dir-uri "https://github.com/greglook/vault/blob/develop/"
     :src-linenum-anchor-prefix "L"}

    :aliases
    {"docs" ["do" "hiera" "doc" ["marg" "--multi" "--dir" "target/doc/marginalia"]]}}

   :tool
   {:source-paths ["tool"]
    :dependencies
    [[mvxcvi/directive "0.4.2"]]
    :jvm-opts []}})
