(ns asset-minifier.core
  (:require [clojure.java.io :refer [file reader writer make-parents]]
            [clojure.string :as string]
            [clj-html-compressor.core :as html-comressor]
            [asset-minifier.spec :as spec])
  (:import com.yahoo.platform.yui.compressor.CssCompressor
           java.util.zip.GZIPOutputStream
           java.io.FileInputStream
           java.io.FileOutputStream
           java.io.File
           org.apache.commons.io.IOUtils
           java.util.logging.Level
           java.util.Map
           java.nio.charset.Charset
           clojure.lang.Sequential
           [com.google.javascript.jscomp
            CompilationLevel
            CompilerOptions
            SourceFile
            CompilerOptions$LanguageMode
            CommandLineRunner]))

(def js ".js")
(def css ".css")
(def html ".html")
(def gz ".gz")

(defn- delete-target [target]
  (let [f (file target)]
    (when (.exists f)
      (.delete f))
    (make-parents f)))

(defn- find-assets [folder ext]
  (if-not (.isDirectory folder)
    [folder]
    (->> folder
         (file-seq)
         (filter #(string/ends-with? (.getName %) ext)))))

(defn- aggregate [paths ext]
  (if-not (coll? paths)
    (aggregate [paths] ext)
    (->> paths
         (map #(find-assets (file %) ext))
         (flatten))))

(defn- aggregate-with-base-path
  [paths ext]
  (if-not (coll? paths)
    (aggregate-with-base-path [paths] ext)
    (mapcat (fn [path]
              (let [f (file path)]
                (->> (find-assets f ext)
                     (map #(vector % f)))))
            paths)))

(defn- relative-path
  [base asset]
  (if (.isFile base)
    (.getName asset)
    (->> asset
         (.toPath)
         (.relativize (.toPath base))
         (.toString))))

(defn total-size [files]
  (->> files
       (map #(.length %))
       (apply +)))

(defn- create-temp-file [source]
  (File/createTempFile (.getName source) gz))

(defn- create-temp-files [sources]
  (map create-temp-file sources))

(defn- gzip [file-in file-out]
  (with-open [in (FileInputStream. file-in)
              out (FileOutputStream. file-out)
              outGZIP (GZIPOutputStream. out)]
    (IOUtils/copy in outGZIP)))

(defmulti compression-details (fn [& args] (mapv class args)))
(defmethod compression-details [Sequential File] [sources target]
  (let [tmp (create-temp-file target)]
    (gzip target tmp)
    (let [uncompressed-length (total-size sources)
          compressed-length   (.length target)]
      {:sources (map #(.getName %) sources)
       :target (.getName target)
       :original-size uncompressed-length
       :compressed-size compressed-length
       :gzipped-size (.length tmp)})))
(defmethod compression-details [Sequential Sequential] [sources targets]
  (let [tmps (create-temp-files sources)]
    (doseq [source sources
            tmp tmps]
      (gzip source tmp))
    (let [uncompressed-length (total-size sources)
          compressed-length (total-size targets)
          gziped-length (total-size tmps)]
      {:sources (map #(.getName %) sources)
       :targets (map #(.getName %) targets) ;; Here i use targets
       :original-size uncompressed-length
       :compressed-size compressed-length
       :gzipped-size gziped-length})))

(defn minify-css-input [source target {:keys [linebreak] :or {linebreak -1}}]
  (with-open [rdr (reader source)
              wrt (writer target)]
    (-> rdr
        (CssCompressor.)
        (.compress wrt linebreak))))

(defn minify-css [path target & [opts]]
  (delete-target target)
  (let [assets (aggregate path css)
        tmp    (File/createTempFile "temp-sources" css)
        target (file target)]
    (with-open [wrt (writer tmp :append true)]
      (doseq [file assets]
        (.append wrt (slurp file))))
    (minify-css-input tmp target opts)
    (compression-details assets target)))

(defn- set-optimization [options optimization]
  (-> optimization
      {:advanced CompilationLevel/ADVANCED_OPTIMIZATIONS
       :simple CompilationLevel/SIMPLE_OPTIMIZATIONS
       :whitespace CompilationLevel/WHITESPACE_ONLY}
      (.setOptionsForCompilationLevel options))
  options)

(defn- parse-language [language]
  (-> language
      (name)
      (.toUpperCase)
      (CompilerOptions$LanguageMode/fromString)))

(defn- prepare-externs [options externs]
  (map #(if (instance? File %)
          (SourceFile/fromFile (.getPath %))
          (SourceFile/fromFile %))
       externs))

(defn- compile-js [compiler assets externs optimization language-in language-out]
  (let [language-in     (parse-language language-in)
        language-out (parse-language language-out)
        options      (-> (doto (CompilerOptions.)
                           (.setLanguageIn language-in)
                           (.setLanguageOut language-out)
                           (.setOutputCharset (Charset/forName "UTF-8")))
                         (set-optimization optimization))]
    (.compile compiler
              (prepare-externs options externs)
              (map #(SourceFile/fromFile (.getPath %)) assets)
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

(defn minify-js [path target & [{:keys [quiet? externs optimization language-in language-out]
                                 :or {quiet? false
                                      language-in :ecmascript6
                                      language-out :ecmascript6
                                      externs []
                                      optimization :simple}}]]
  (delete-target target)
  (if (= :none optimization)
    (merge-files (aggregate path js) target)
    (do
      (if quiet?
        (com.google.javascript.jscomp.Compiler/setLoggingLevel Level/OFF)
        (com.google.javascript.jscomp.Compiler/setLoggingLevel Level/SEVERE))
      (let [assets   (aggregate path js)
            compiler (com.google.javascript.jscomp.Compiler.)
            result   (compile-js compiler assets externs optimization language-in language-out)]
        (spit target (.toSource compiler))
        (merge result (compression-details assets (file target)))))))

(defn- minify-html-asset [asset target base-path opts]
  (let [result (html-comressor/compress (slurp asset) opts)
        target-file (file target (relative-path base-path asset))]
    (make-parents (.getPath target-file))
    (spit target-file result)))

(defn minify-html [path target opts]
  (delete-target target)
  (let [assets (aggregate-with-base-path path html)]
    (doseq [[asset base-path] assets]
      (minify-html-asset asset target base-path opts))
    (compression-details (map first assets) (aggregate target html))))

(defn get-minifier-fn-by-type [asset-type]
  (case asset-type
    :html minify-html
    :css minify-css
    :js minify-js))

(defn minify
  "assers are specified using a vector of configs
  [[:html {:source \"dev/resource/html\" :target \"dev/minified/html\"}]
   [:css {:source \"dev/resources/css\" :target \"dev/minified/css/styles.min.css\"}]
   [:js {:source [\"dev/res/js1\", \"dev/res/js2\"] :target \"dev/minified/js/script.min.js\"}]]"
  [config]
  {:pre [(spec/is-valid-config config)]}
  (for [[asset-type opts] config]
    (let [minify-fn (get-minifier-fn-by-type asset-type)]
      (minify-fn (:source opts) (:target opts) (:opts opts)))))
