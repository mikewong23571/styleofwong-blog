(ns blog.governance-test
  (:require [cljs.test :refer [deftest is]]
            [blog.governance.core :as gov]
            [blog.publish :as publish]))

(deftest rejects-unknown-tags
  (let [publish-ir (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")
        report (gov/run-governance publish-ir {:tags #{"clojurescript" "astro" "content-system"}})]
    (is (= [] (:errors report)))))

(deftest rejects-missing-required-fields
  (let [publish-ir {:pages [{:slug "broken"
                             :url "/posts/broken/"
                             :tags ["clojurescript"]
                             :status "published"
                             :published_at "2026-04-19"}]}
        report (gov/run-governance publish-ir {:tags #{"clojurescript"}})]
    (is (= #{:missing-title :missing-created-at}
           (into #{} (map :rule (:errors report)))))))

(deftest rejects-invalid-lifecycle-and-duplicate-urls
  (let [publish-ir {:pages [{:slug "a"
                             :title "A"
                             :created_at "2026-04-19"
                             :status "published"
                             :tags ["clojurescript"]
                             :url "/posts/shared/"
                             :published_at nil
                             :metadata-entry {:overrides {:tags ["clojurescript"]
                                                          :summary "A"}}}
                            {:slug "b"
                             :title "B"
                             :created_at "2026-04-19"
                             :status "draft"
                             :tags ["clojurescript"]
                             :url "/posts/shared/"
                             :published_at "2026-04-19"
                             :metadata-entry {:overrides {:tags ["clojurescript"]
                                                          :summary "B"}}}]}
        report (gov/run-governance publish-ir {:tags #{"clojurescript"}})]
    (is (= #{:published-missing-published-at
             :draft-has-published-at
             :duplicate-url}
           (into #{} (map :rule (:errors report)))))))

(deftest rejects-missing-metadata-overrides
  (let [publish-ir {:pages [{:slug "broken"
                             :title "Broken"
                             :created_at "2026-04-19"
                             :status "published"
                             :published_at "2026-04-19"
                             :tags ["clojurescript"]
                             :url "/posts/broken/"
                             :metadata-entry {:llm {:tags ["clojurescript"]}}}]}
        report (gov/run-governance publish-ir {:tags #{"clojurescript"}})]
    (is (= [:missing-metadata-overrides]
           (mapv :rule (:errors report))))))
