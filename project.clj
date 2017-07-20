(defproject asset-minifier "0.2.4"
  :description "a library to minify CSS and Js sources"
  :url "https://github.com/yogthos/asset-minifier"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-future-spec "1.9.0-alpha17"]
                 [org.clojure/test.check "0.9.0"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.8" :exclusions [rhino/js]]
                 [com.google.javascript/closure-compiler "v20160208"]
                 [clj-html-compressor "0.1.1"]
                 [commons-io "2.5"]]
  :profiles {:dev {:dependencies [[pjstadig/humane-test-output "0.8.1"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}})
