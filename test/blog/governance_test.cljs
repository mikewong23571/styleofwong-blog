(ns blog.governance-test
  (:require [cljs.test :refer [deftest is]]
            [blog.governance.core :as gov]
            [blog.publish :as publish]))

(deftest rejects-unknown-tags
  (let [publish-ir (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")
        report (gov/run-governance publish-ir {:tags #{"clojurescript" "astro" "content-system"}})]
    (is (= [] (:errors report)))))
