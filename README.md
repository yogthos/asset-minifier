# asset-minifier

a Clojure library to minify CSS and Js resources

## Usage

Dependency

```clojure
[]
```

The minifier provides two functions called `minify-css` and `minify-js`, both of these functions accept source path followed by the output target and an optional parameter map. Both functions can take a directory as an input parameter and will minify all the resources found in the directory and subdirectories.

The `minify-css` function allows specifying `:linebreak` to force line breaks after a certain number of chracters in the minified CSS.

```clojure
(ns my.ns
  (:require [asset-minifier.core :refer [minify-css minfy-js]]))

;minify the site.css into site.min.css
(minify-css "site.css" "site.min.css")


(minify-css "site.css" "site.min.css" {:linebreak 80})

;minify all css resources into site.min.css
(minify-css "dev/resources/css" "resources/public/css/site.min.css")
```

The function returns a map containing `:original-size` and `:compressed-size` keys indicating the size of the assets before and after compression.


The `minify-js` function allows specifying the level of optimizations, which can be `:simple`, `:whitespace` or `:advanced` and defaults to simple optimizations.

The `:externs` key can be used to specify the externs file to be used with the advanced optimisations to prevent munging of external functions.

```clojure
;minify site.js into site.min.js
(minify-js "site.js" "site.min.js")

;minify site.js into site.min.js with advanced optimizations and jquery externs
(minify-js "site.js" "site.min.js" {:optimization :advanced
                                    :externs ["jquery.min.js"]})

;minify all Js resources into site.min.js
(minify-js "dev/resources/js" "resources/public/js/site.min.js")
```

The function returns a map containing `:original-size` and `:compressed-size` keys indicating the size of the assets before and after compression. In addition the map may contain `:warnings` and `:errors` keys to indicate any warnings or errors that were issued during compilation.


## License

Copyright © 2014 Yogthos

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
