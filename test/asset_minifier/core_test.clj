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

(defn round-gzipped [m]
  (update-in m [:gzipped-size] #(when % (int (/ % 100)))))

(defmacro run-test [fn result]
  `(do
     (clean-output (file output-path))
     (is (= (round-gzipped ~result) (round-gzipped ~fn)))))

(deftest test-minification

  (testing "testing CSS minifcation"

    ;; minify directory
    (run-test
      (minify-css input-path (str output-path "output.min.css"))
      {:sources '("input1.css" "input2.css")
       :target "output.min.css"
       :original-size 3883
       :compressed-size 3068
       :gzipped-size 1022})

    ;; minify a file
    (run-test
      (minify-css (str input-path "/css/input1.css") (str output-path "output.min.css"))
      {:sources '("input1.css"),
       :target "output.min.css",
       :original-size 989,
       :compressed-size 783,
       :gzipped-size 423})

    ;; minify a file into non-existent directory
    (run-test
      (minify-css (str input-path "/css/input1.css") (str output-path "missing-css-dir/output.min.css"))
      {:sources '("input1.css"),
       :target "output.min.css",
       :original-size 989,
       :compressed-size 783,
       :gzipped-size 423})
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
      {:gzipped-size 808,
       :compressed-size 1804,
       :original-size 2547,
       :target "output.min.js",
       :sources '("externs.js" "input1.js" "input2.js"),
       :warnings (),
       :errors '()})

    ;; minify a file
    (run-test
      (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js"))
      {:gzipped-size 93,
       :compressed-size 84,
       :original-size 117,
       :target "output.min.js",
       :sources '("input1.js"),
       :warnings '(),
       :errors '()})

    ;; minify a file into non-existent directory
    (run-test
      (minify-js (str input-path "/js/input1.js") (str output-path "missing-js-dir/output.min.js"))
      {:gzipped-size 93,
       :compressed-size 84,
       :original-size 117,
       :target "output.min.js",
       :sources '("input1.js"),
       :warnings '(),
       :errors '()})
    (is (= true (.exists (file (str output-path "missing-js-dir/output.min.js")))))

    ;; minify a file without optimization
    (run-test
      (minify-js (str input-path "/js/input1.js") (str output-path "output.min.js")
        {:optimization :none})
      {:sources '("input1.js"),
       :target "output.min.js",
       :original-size 117})

    ;; minify a file with advanced optimization
    (run-test
      (minify-js (str input-path "/js/input2.js") (str output-path "output.min.js")
        {:optimization :advanced :externs [(str input-path "/js/externs.js")]})
      {:gzipped-size 628,
       :compressed-size 1265,
       :original-size 2409,
       :target "output.min.js",
       :sources '("input2.js"),
       :warnings '("JSC_UNDEFINED_EXTERN_VAR_ERROR. name hljs is not defined in the externs. at test/resources/js/externs.js line 1 : 0"),
       :errors '()})))
