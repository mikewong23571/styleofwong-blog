(ns blog.main
  (:require [blog.governance.core :as gov]
            [blog.publish :as publish]
            [blog.targets.astro :as astro]))

(def governance-policy
  {:tags #{"clojurescript" "astro" "content-system"}})

(defn ^:export main []
  (let [publish-ir (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")]
    (let [report (gov/run-governance publish-ir governance-policy)]
      (when-not (:ok? report)
        (throw (js/Error. "Governance failed."))))
    (astro/emit-runtime-config! "site/astro/managed/runtime-config.json" publish-ir)
    (astro/emit-posts-data! "site/astro/src/data/posts.json" publish-ir)
    (doseq [page (:pages publish-ir)]
      (astro/emit-post-page! (str "site/astro/src/pages/posts/" (:slug page) ".mdx") page)
      (astro/emit-assets! "site/astro/public/generated-assets" page))
    (println "Managed Astro artifacts emitted.")))
