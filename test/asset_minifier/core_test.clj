(ns asset-minifier.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file delete-file]]
            [asset-minifier.core :refer :all]))

(def input-path "test/resources")
(def output-path "target/minified/")

(defn clean-output [file]
  (if (.isDirectory file)
    (when (reduce #(and %1 (clean-output %2)) true (.listFiles file))
      (.delete file))
    (.delete file)))

(defn massage-data [m]
  (-> m
      (update-in [:sources] set)
      (update-in [:gzipped-size] #(when % %))))

(defmacro run-test [fn result]
  `(do
     (clean-output (file output-path))
     (is (= (massage-data ~result) (massage-data ~fn)))))

(deftest test-minification

  (testing "testing CSS minifcation"

    ;; minify directory
    (run-test
     (minify-css input-path (str output-path "output.min.css"))
     {:sources '("input1.css" "input2.css")
      :target "output.min.css"
      :original-size 3883
      :compressed-size 3069
      :gzipped-size 1022})

    ;; minify a file
    (run-test
     (minify-css (str input-path "/css/input1.css") (str output-path "output.min.css"))
     {:sources '("input1.css"),
      :target "output.min.css",
      :original-size 989,
      :compressed-size 784,
      :gzipped-size 424})

    ;; minify a file into non-existent directory
    (run-test
     (minify-css (str input-path "/css/input1.css") (str output-path "missing-css-dir/output.min.css"))
     {:sources '("input1.css"),
      :target "output.min.css",
      :original-size 989,
      :compressed-size 784,
      :gzipped-size 424})
    (is (= true (.exists (file (str output-path "missing-css-dir/output.min.css")))))

    ;; minify a file with custom linebreak
    (run-test
     (minify-css (str input-path "/css/input2.css") (str output-path "output.min.css") {:linebreak 80})
     {:sources '("input2.css"),
      :target "output.min.css",
      :original-size 2894,
      :compressed-size 2301,
      :gzipped-size 725}))

  (testing "testing Js minifcation"

    ;; minify directory
    (run-test
     (minify-js input-path (str output-path "output.min.js"))
     {:warnings ()
      :errors ()
      :sources #{"externs.js" "input1.js" "input2.js" "input3.js" "async.js"}
      :target "output.min.js"
      :original-size 3354
      :compressed-size 2279
      :gzipped-size 1019})

    ;; minify a file
    (run-test
     (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js"))
     {:warnings ()
      :errors ()
      :sources #{"input1.js"}
      :target "output.min.js"
      :original-size 117
      :compressed-size 97
      :gzipped-size 105})

    ;; minify a file into non-existent directory
    (run-test
     (minify-js (str input-path "/js/input1.js") (str output-path "missing-js-dir/output.min.js"))
     {:warnings ()
      :errors ()
      :sources #{"input1.js"}
      :target "output.min.js"
      :original-size 117
      :compressed-size 97
      :gzipped-size 105})
    (is (= true (.exists (file (str output-path "missing-js-dir/output.min.js")))))

    ;; minify a file without optimization
    (run-test
     (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js")
                {:optimization :none})
     {:sources #{"input1.js"}
      :target "output.min.js"
      :original-size 117})

    ;; minify a file with advanced optimization
    (run-test
     (minify-js (str input-path "/js/input2.js") (str output-path "output.min.js")
                {:optimization :advanced :externs [(str input-path "/js/externs.js")]})
     {:warnings
      ["JSC_UNDEFINED_EXTERN_VAR_ERROR. name hljs is not defined in the externs. at test/resources/js/externs.js line 1 : 0"],
      :errors
      ["JSC_UNDEFINED_VARIABLE. variable hljs is undeclared at test/resources/js/externs.js line 1 : 0"
       "JSC_UNDEFINED_VARIABLE. variable $ is undeclared at test/resources/js/input2.js line 1 : 0"
       "JSC_UNDEFINED_VARIABLE. variable document is undeclared at test/resources/js/input2.js line 1 : 2"
       "JSC_UNDEFINED_VARIABLE. variable navigator is undeclared at test/resources/js/input2.js line 3 : 55"
       "JSC_UNDEFINED_VARIABLE. variable SyntaxHighlighter is undeclared at test/resources/js/input2.js line 10 : 1"
       "JSC_UNDEFINED_VARIABLE. variable jConfirm is undeclared at test/resources/js/input2.js line 34 : 1"
       "JSC_UNDEFINED_VARIABLE. variable markdown is undeclared at test/resources/js/input2.js line 45 : 26"
       "JSC_UNDEFINED_VARIABLE. variable window is undeclared at test/resources/js/input2.js line 52 : 4"
       "JSC_UNDEFINED_VARIABLE. variable e is undeclared at test/resources/js/input2.js line 54 : 9"
       "JSC_UNDEFINED_VARIABLE. variable context is undeclared at test/resources/js/input2.js line 75 : 14"
       "JSC_UNDEFINED_VARIABLE. variable Date is undeclared at test/resources/js/input2.js line 85 : 78"],
      :sources #{"input2.js"},
      :target "output.min.js",
      :original-size 2409,
      :compressed-size 1837,
      :gzipped-size 826}))

  (testing "html minification"
    (run-test
     (minify-html (str input-path "/html/") (str output-path "html/") {})
     {:sources '("testCompress.html") :targets '("testCompress.html") :original-size 581 :compressed-size 459 :gzipped-size 256}))

  (testing "minify function"
    (is (= (minify [[:html {:source (str input-path "/html/") :target (str output-path "html/")}]
                    [:js {:source (str input-path "/js/input1.js") :target (str output-path "output.min.js")}]
                    [:css {:source (str input-path "/css/input1.css") :target (str output-path "output.min.css")}]])
            [{:sources '("testCompress.html")
              :targets '("testCompress.html")
              :original-size 581
              :compressed-size 459
              :gzipped-size 256}
             {:warnings ()
              :errors ()
              :sources '("input1.js")
              :target "output.min.js"
              :original-size 117
              :compressed-size 97
              :gzipped-size 105}
             {:sources '("input1.css"),
              :target "output.min.css",
              :original-size 989,
              :compressed-size 784,
              :gzipped-size 424}]))))
