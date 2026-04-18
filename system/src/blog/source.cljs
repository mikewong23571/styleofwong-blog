(ns blog.source)

(defn discover-posts [root]
  [{:slug "hello-world"
    :bucket-year "2026"
    :markdown-path (str root "/2026/hello-world/index.md")
    :asset-dir (str root "/2026/hello-world")}])
