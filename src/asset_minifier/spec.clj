(ns asset-minifier.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::path (s/or :path string?
                    :paths (s/+ string?)))
(s/def ::source ::path)
(s/def ::target ::path)
(s/def ::opts map?)
(s/def ::config-body (s/keys :req-un [::source ::target]
                             :opt-un [::opts]))
(s/def ::asset-type #{:html :css :js})
(s/def ::config-item (s/cat :asset-type ::asset-type :config-body ::config-body))
(s/def ::config (s/coll-of ::config-item))

(defn is-valid-config [config]
  (s/valid? ::config config))

(defn explain-config [config]
  (s/explain ::config config))
