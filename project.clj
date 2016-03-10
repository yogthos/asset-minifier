(defproject asset-minifier "0.1.8-SNAPSHOT"
  :description "a library to minify CSS and Js sources"
  :url "https://github.com/yogthos/asset-minifier"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.7" :exclusions [rhino/js]]
                 [com.google.javascript/closure-compiler "v20160208"]
                 [commons-io "2.4"]]
  :profiles {:dev {:dependencies [[pjstadig/humane-test-output "0.6.0"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}})
