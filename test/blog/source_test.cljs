(ns blog.source-test
  (:require [cljs.test :refer [deftest is]]
            [blog.source :as source]))

(deftest discovers-posts-with-storage-year-but-stable-slug
  (let [items (source/discover-posts "content/posts")]
    (is (= ["hello-world"] (mapv :slug items)))
    (is (= "2026" (:bucket-year (first items))))
    (is (= "content/posts/2026/hello-world/index.md" (:markdown-path (first items))))))
