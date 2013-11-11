(defproject mvxcvi/vault "0.3.0-SNAPSHOT"
  :description "Content-addressible datastore."
  :url "https://github.com/greglook/vault"
  :license {:name "Public Domain"
            :url "http://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.cli "0.2.4"]
                 [org.bouncycastle/bcpg-jdk15on "1.49"]
                 [mvxcvi/directive "0.1.0"]
                 [mvxcvi/puget "0.1.0"]
                 [digest "1.4.3"]
                 [fipp "0.4.1"]]
  :main vault.tool)
