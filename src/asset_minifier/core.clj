(ns asset-minifier.core
  (:require [clojure.java.io :refer [file reader writer make-parents]])
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
            SourceFile
            CompilerOptions$LanguageMode]))

(defn- delete-target [target]
  (let [f (file target)]
    (when (.exists f)
      (.delete f))
    (make-parents f)))

(defn- find-assets [f ext]
  (if (.isDirectory f)
    (->> f
         file-seq
         (filter (fn [file] (-> file .getName (.endsWith ext)))))
    [f]))

(defn- aggregate [path ext]
  (if (coll? path)
   (flatten
     (for [item path]
      (let [f (file item)]
        (find-assets f ext))))
    (let [f (file path)]
      (find-assets f ext))))

(defn total-size [files]
  (->> files (map #(.length %)) (apply +)))

(defn- compression-details [sources target]
  (let [tmp (File/createTempFile (.getName target) ".gz")]
    (with-open [in  (FileInputStream. target)
                out (FileOutputStream. tmp)
                outGZIP (GZIPOutputStream. out)]
      (IOUtils/copy in outGZIP))
    (let [uncompressed-length (total-size sources)
            compressed-length   (.length target)]
        {:sources (map #(.getName %) sources)
         :target (.getName target)
         :original-size uncompressed-length
         :compressed-size compressed-length
         :gzipped-size (.length tmp)})))

(defn- minify-css-file [source target {:keys [linebreak] :or {linebreak -1}}]
  (with-open [rdr (reader source)
              wrt (writer target)]
    (-> rdr (CssCompressor.) (.compress wrt linebreak))))

(defn minify-css [path target & [opts]]
  (delete-target target)
  (let [assets (aggregate path ".css")
        tmp    (File/createTempFile "temp-sources" ".css")
        target (file target)]
   (with-open [wrt (writer tmp :append true)]
     (doseq [file assets]
       (.append wrt (slurp file))))
    (minify-css-file tmp target opts)
    (compression-details assets target)))

(defn- set-optimization [options optimization]
  (-> optimization
      {:advanced CompilationLevel/ADVANCED_OPTIMIZATIONS
       :simple CompilationLevel/SIMPLE_OPTIMIZATIONS
       :whitespace CompilationLevel/WHITESPACE_ONLY}
      (.setOptionsForCompilationLevel options))
  options)

(defn- compile-js [compiler assets externs optimization language]
  (let [options  (-> (CompilerOptions.)
                     (doto (.setLanguage (-> language
                                             (name)
                                             (.toUpperCase)
                                             (CompilerOptions$LanguageMode/fromString))))
                     (doto (.setOutputCharset "UTF-8"))
                     (set-optimization optimization))]

    (.compile compiler
      (map #(SourceFile/fromFile %) externs)
      (map #(SourceFile/fromFile %) assets)
      options)
    {:warnings (map #(.toString %) (.getWarnings compiler))
     :errors   (map #(.toString %) (.getErrors compiler))}))

(defn merge-files [sources target]
  (with-open [out (FileOutputStream. target)]
    (doseq [file sources]
      (with-open [in (FileInputStream. file)]
        (IOUtils/copy in out))))
  {:sources (map #(.getName %) sources)
   :target (.getName (file target))
   :original-size (total-size sources)})

(defn minify-js [path target & [{:keys [quiet? externs optimization language]
                                 :or {quiet? false
                                      language :ecmascript3
                                      externs []
                                      optimization :simple}}]]
  (delete-target target)
  (if (= :none optimization)
    (merge-files (aggregate path ".js") target)
    (do
      (if quiet?
        (com.google.javascript.jscomp.Compiler/setLoggingLevel Level/OFF)
        (com.google.javascript.jscomp.Compiler/setLoggingLevel Level/SEVERE))
      (let [assets   (aggregate path ".js")
            compiler (com.google.javascript.jscomp.Compiler.)
            result   (compile-js compiler assets externs optimization language)]
        (with-open [wrt (writer target)]
          (.append wrt (.toSource compiler)))
        (merge result (compression-details assets (file target)))))))

(defn minify
  "assets are specified using a map where the key is the output file and the value is the asset paths, eg:
   {\"site.min.css\" \"dev/resources/css\"
    \"site.min.js\" \"dev/resources/js/site.js\"
    \"vendor.min.js\" [\"dev/resources/vendor1/js\"
                       \"dev/resources/vendor2/js\"]}"
  [assets & [opts]]
  (into {}
   (for [[target path] assets]
     [[path target]
      (cond
        (.endsWith target ".js")  (minify-js path target opts)
        (.endsWith target ".css") (minify-css path target opts)
        :else (throw (ex-info "unrecognized target" target)))])))
