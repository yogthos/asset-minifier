(ns asset-minifier.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [asset-minifier.spec :as spec]))

(deftest spec-test
  (testing "path spec"
    (is (s/valid? ::spec/path "path"))
    (is (s/valid? ::spec/path ["vector" "of" "paths"]))
    (is (not (s/valid? ::spec/path :some-thing-else))))
  
  (testing "config body"
    (is (s/valid? ::spec/config-body {:source "str" :target "str"}))
    (is (s/valid? ::spec/config-body {:source ["str"] :target ["str"]}))
    (is (not (s/valid? ::spec/config-body {:source "source"})))
    (is (not (s/valid? ::spec/config-body {:target "target"}))))
  
  (testing "asset type"
    (is (s/valid? ::spec/asset-type :html))
    (is (s/valid? ::spec/asset-type :css))
    (is (s/valid? ::spec/asset-type :js))
    (is (not (s/valid? ::spec/asset-type :not-known))))
  
  (testing "is-valid-config"
    (is (spec/is-valid-config [[:html {:source "str" :target "str"}]]))
    (is (spec/is-valid-config [[:html {:source "str" :target "str"}]
                               [:css {:source ["str1" "str2"] :target "str"}]
                               [:js {:source "str" :target ["str1" "str2"]}]]))
    (is (not (spec/is-valid-config [[]])))
    (is (not (spec/is-valid-config [[:html {}]])))))

