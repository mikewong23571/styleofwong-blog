(ns blog.main
  (:require [blog.publish :as publish]
            [blog.targets.astro :as astro]))

(defn ^:export main []
  (let [publish-ir (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")]
    (astro/emit-runtime-config! "site/astro/managed/runtime-config.json" publish-ir)
    (astro/emit-posts-data! "site/astro/src/data/posts.json" publish-ir)
    (astro/emit-post-page! "site/astro/src/pages/posts/hello-world.mdx")
    (println "Managed Astro artifacts emitted.")))
