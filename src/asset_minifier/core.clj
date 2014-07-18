(ns asset-minifier.core
  (:require [clojure.java.io :refer [file reader writer]])
  (:import com.yahoo.platform.yui.compressor.CssCompressor
           java.util.zip.GZIPOutputStream
           java.io.FileInputStream
           java.io.FileOutputStream
           java.io.File
           org.apache.commons.io.IOUtils
           java.util.logging.Level
           [com.google.javascript.jscomp
            CompilationLevel
            CompilerOptions
            JSSourceFile]))

(defn- find-sources [path ext]
  (->> path
       file-seq
       (filter (fn [file] (-> file .getName (.endsWith ext))))))

(defn- compression-details [sources target]
  (let [tmp (File/createTempFile (.getName target) ".gz")]
    (with-open [in  (FileInputStream. target)
                out (FileOutputStream. tmp)
                outGZIP (GZIPOutputStream. out)]
      (IOUtils/copy in outGZIP)
      {:original-szie (str "Uncompressed size: " (->> sources (map #(.length %)) (apply +)) " bytes")
       :compressed-size (str "Compressed size: "(.length target) " bytes minified (" (.length tmp) " bytes gzipped)")})))

(defn- minify-css-file [source target {:keys [linebreak] :or {linebreak -1}}]
  (with-open [rdr (reader source)
              wrt (writer target)]
    (-> rdr (CssCompressor.) (.compress wrt linebreak))))

(defn- minify-css-at-path [path target opts]
  (let [tmp     (File/createTempFile "temp-sources" ".css")
        sources (find-sources (file path) ".css")
        target  (file target)]
   (with-open [wrt (writer tmp :append true)]
     (doseq [file (find-sources (file path) ".css")]
       (.append wrt (slurp file))))
    (minify-css-file tmp target opts)
    (compression-details sources target)))

(defn minify-css [path target & opts]
  (let [path (file path)]
    (if (.isDirectory path)
      (minify-css-at-path path target opts)
      (do
        (minify-css-file path target opts)
        (compression-details [path] (file target))))))

(defn- set-optimization [optimization options]
  (-> optimization
      {:advanced CompilationLevel/ADVANCED_OPTIMIZATIONS
       :simple CompilationLevel/SIMPLE_OPTIMIZATIONS
       :whitespace CompilationLevel/WHITESPACE_ONLY}
      (.setOptionsForCompilationLevel options)))

(defn- compile-js [compiler sources externs optimization]
  (let [options  (doto (CompilerOptions.) (.setOutputCharset "UTF-8"))]
    (set-optimization optimization options)
    (com.google.javascript.jscomp.Compiler/setLoggingLevel Level/SEVERE)
    (.compile compiler
      (map #(JSSourceFile/fromFile %) externs)
      (map #(JSSourceFile/fromFile %) sources)
      options)
    {:warnings (map #(.toString %) (.getWarnings compiler))
     :errors   (map #(.toString %) (.getErrors compiler))}))

(defn minify-js [path target & [{:keys [externs optimization]
                                 :or {externs []
                                      optimization :simple}}]]
  (let [path     (file path)
        sources  (if (.isDirectory path) (find-sources path ".js") [path])
        compiler (com.google.javascript.jscomp.Compiler.)
        result   (compile-js compiler sources externs optimization)]
    (with-open [wrt (writer target)]
      (.append wrt (.toSource compiler)))
    (merge
     result
     (compression-details sources (file target)))))
