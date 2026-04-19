(ns blog.source-test
  (:require ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [deftest is]]
            [blog.source :as source]))

(defn make-temp-root! []
  (.mkdtempSync fs (.join path (.tmpdir os) "blog-source-test-")))

(defn mkdirp! [dir]
  (.mkdirSync fs dir #js {:recursive true}))

(defn write-file! [file content]
  (mkdirp! (.dirname path file))
  (.writeFileSync fs file content "utf8"))

(deftest discovers-posts-with-storage-year-but-stable-slug
  (let [root (make-temp-root!)
        valid-2027 (.join path root "2027" "second-post")
        valid-2026 (.join path root "2026" "first-post")
        ignored (.join path root "2025" "draft-post")]
    (write-file! (.join path valid-2027 "index.md") "# Second")
    (write-file! (.join path valid-2027 "chart.svg") "<svg />")
    (write-file! (.join path valid-2026 "index.md") "# First")
    (write-file! (.join path ignored "notes.txt") "ignore me")
    (let [items (source/discover-posts root)]
      (is (= ["first-post" "second-post"] (mapv :slug items)))
      (is (= ["2026" "2027"] (mapv :bucket-year items)))
      (is (= [(.join path root "2026" "first-post" "index.md")
              (.join path root "2027" "second-post" "index.md")]
             (mapv :markdown-path items)))
      (is (= [(.join path root "2026" "first-post")
              (.join path root "2027" "second-post")]
             (mapv :asset-dir items))))))
