(ns asset-minifier.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [asset-minifier.core :refer :all]))

(def input-path "test/resources")
(def output-path "test/resources/minified/")

(defn clean-output []
  (doseq [f (-> output-path file file-seq rest)]
    (.delete f)))

(defmacro run-test [t result]
  `(do
     (clean-output)
     (is
      (= ~result ~t))))

(deftest test-minification
  (testing "testing CSS minifcation"
    (run-test
      (minify-css input-path (str output-path "output.min.css"))
      {:original-szie "Uncompressed size: 3883 bytes"
       :compressed-size "Compressed size: 3068 bytes minified (10 bytes gzipped)"})
    (run-test
     (minify-css (str input-path "/css/input1.css") (str output-path "output.min.css"))
     {:original-szie "Uncompressed size: 989 bytes", :compressed-size "Compressed size: 783 bytes minified (10 bytes gzipped)"})

    (run-test
     (minify-css (str input-path "/css/input1.css") (str output-path "output.min.css") {:linebreak 80})
     {:original-szie "Uncompressed size: 989 bytes", :compressed-size "Compressed size: 788 bytes minified (10 bytes gzipped)"}))

  (testing "testing Js minifcation"
    (run-test
     (minify-js input-path (str output-path "output.min.js"))
     {:compressed-size "Compressed size: 1804 bytes minified (10 bytes gzipped)"
      :original-szie "Uncompressed size: 2547 bytes"
      :warnings []
      :errors []})
    (run-test
     (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js"))
     {:compressed-size "Compressed size: 84 bytes minified (10 bytes gzipped)"
      :original-szie "Uncompressed size: 117 bytes"
      :warnings []
      :errors []})
    (run-test
     (minify-js input-path (str output-path "output.min.js") {:optimizations :whitespace})
     {:compressed-size "Compressed size: 1804 bytes minified (10 bytes gzipped)"
      :original-szie "Uncompressed size: 2547 bytes"
      :warnings []
      :errors []})
    (run-test
      (minify-js (str input-path "/js/input2.js")
                 (str output-path "output.min.js")
                 {:optimizations :advanced
                  :externs [(str input-path "/js/externs.js")]})
      {:compressed-size "Compressed size: 1700 bytes minified (10 bytes gzipped)"
       :original-szie "Uncompressed size: 2409 bytes"
       :warnings ["JSC_UNDEFINED_EXTERN_VAR_ERROR. name hljs is not defined in the externs. at test/resources/js/externs.js line 1 : 0"]
       :errors []})))
