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
      {:original-szie 3883
       :compressed-size 3068
       :summary "Uncompressed size:  bytes\nCompressed size: 3068 bytes minified (1022 bytes gzipped)"})
    (run-test
     (minify-css (str input-path "/css/input1.css") (str output-path "output.min.css"))
     {:original-szie 989
      :compressed-size 783
      :summary "Uncompressed size:  bytes\nCompressed size: 783 bytes minified (423 bytes gzipped)"})

    (run-test
     (minify-css (str input-path "/css/input2.css") (str output-path "output.min.css") {:linebreak 80})
     {:original-szie 2894
      :compressed-size 2301
      :summary "Uncompressed size:  bytes\nCompressed size: 2301 bytes minified (725 bytes gzipped)"}))

  (testing "testing Js minifcation"
    (run-test
     (minify-js input-path (str output-path "output.min.js"))
     {:summary "Uncompressed size:  bytes\nCompressed size: 1804 bytes minified (808 bytes gzipped)"
      :compressed-size 1804
      :original-szie 2547
      :warnings []
      :errors []})
    (run-test
     (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js"))
     {:summary "Uncompressed size:  bytes\nCompressed size: 84 bytes minified (93 bytes gzipped)"
      :compressed-size 84
      :original-szie 117
      :warnings []
      :errors []})
    (run-test
     (minify-js input-path (str output-path "output.min.js") {:optimizations :whitespace})
     {:summary "Uncompressed size:  bytes\nCompressed size: 1804 bytes minified (808 bytes gzipped)"
      :compressed-size 1804
      :original-szie 2547
      :warnings []
      :errors []})
    (run-test
      (minify-js (str input-path "/js/input2.js")
                 (str output-path "output.min.js")
                 {:optimizations :advanced
                  :externs [(str input-path "/js/externs.js")]})
      {:summary "Uncompressed size:  bytes\nCompressed size: 1700 bytes minified (776 bytes gzipped)"
       :compressed-size 1700
       :original-szie 2409
       :warnings ["JSC_UNDEFINED_EXTERN_VAR_ERROR. name hljs is not defined in the externs. at test/resources/js/externs.js line 1 : 0"]
       :errors []})))
