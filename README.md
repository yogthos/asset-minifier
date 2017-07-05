# asset-minifier

[![Continuous Integration status](https://secure.travis-ci.org/yogthos/asset-minifier.png)](http://travis-ci.org/yogthos/asset-minifier)

A Clojure library to minify HTML, CSS and Js resources, for a Leiningen plugin see [lein-asset-minifier](https://github.com/yogthos/lein-asset-minifier).

The latest version requires ClojureScript 1.9+. If you're on an older version of ClojureScript, then use version `0.2.0` instead.

## Usage

[![Clojars Project](http://clojars.org/asset-minifier/latest-version.svg)](http://clojars.org/asset-minifier)

The minifier provides three functions called `minify-html`, `minify-css` and `minify-js`, all functions accept a source path followed by the output target and an optional parameter map. The source can be a filename, a directory, or a sequence of directories and or filenames.

```clojure
(ns my.ns
  (:require [asset-minifier.core :refer [minify-html minify-css minfy-js]]))
```

The `minify-html` function allows specifying config for htmlcompressor. Please refer too [clj-html-compressor](https://github.com/Atsman/clj-html-compressor).

```clojure
; minify html files from folder /html into /minified
(minify-html "html" "minified")
```

The `minify-css` function allows specifying `:linebreak` to force line breaks after a certain number of chracters in the minified CSS.

```clojure
;minify the site.css into site.min.css
(minify-css "site.css" "site.min.css")

;minify the site.css file and any files found under dev/resources/css
(minify-css ["site.css" "dev/resources/css"] "site.min.css")

(minify-css "site.css" "site.min.css" {:linebreak 80})

;minify all css resources into site.min.css
(minify-css "dev/resources/css" "resources/public/css/site.min.css")
```
The function returns a map containing `:original-size`, `:compressed-size`, and `:gzipped-size` keys indicating the size of the assets before and after compression.


The `minify-js` function allows specifying the level of optimizations, which can be `:none`, `:simple`, `:whitespace` or `:advanced` and defaults to simple optimizations.

The `:externs` key can be used to specify the externs file to be used with the advanced optimisations to prevent munging of external functions.

```clojure
;minify site.js into site.min.js
(minify-js "site.js" "site.min.js")

;minify files found under dev/resources/js and dev/resources/vendor into site.min.js
(minify-js ["dev/resources/js" "dev/resources/vendor"] "site.min.js")

;minify site.js into site.min.js with advanced optimizations and jquery externs
(minify-js "site.js" "site.min.js" {:optimization :advanced
                                    :externs ["jquery.min.js"]})

;minify all Js resources into site.min.js
(minify-js "dev/resources/js" "resources/public/js/site.min.js")
```

The function returns a map containing `:original-size`, `:compressed-size`, and `:gzipped-size` keys indicating the size of the assets before and after compression. In addition the map may contain `:warnings` and `:errors` keys to indicate any warnings or errors that were issued during compilation.

## License

Copyright © 2017 Yogthos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
