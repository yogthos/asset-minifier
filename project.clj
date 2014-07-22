(defproject asset-minifier "0.1.4"
  :description "a library to minify CSS and Js sources"
  :url "https://github.com/yogthos/asset-minifier"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.yahoo.platform.yui/yuicompressor "2.4.7" :exclusions [rhino/js]]
                 [com.google.javascript/closure-compiler "v20131014"]
                 [commons-io "2.4"]])
