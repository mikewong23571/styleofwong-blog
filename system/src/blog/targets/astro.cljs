(ns blog.targets.astro
  (:require ["fs" :as fs]
            ["path" :as path]
            [clojure.string :as str]))

(defn ensure-parent! [file]
  (.mkdirSync fs (.dirname path file) #js {:recursive true}))

(defn emit-runtime-config! [file publish-ir]
  (ensure-parent! file)
  (.writeFileSync fs
                  file
                  (js/JSON.stringify
                   (clj->js {:astro (:runtime publish-ir)
                             :site {:theme "paper"}})
                   nil
                   2)
                  "utf8"))

(defn emit-posts-data! [file publish-ir]
  (ensure-parent! file)
  (.writeFileSync fs
                  file
                  (js/JSON.stringify
                   (clj->js (mapv #(select-keys % [:slug :url :summary :tags :title :created_at :published_at]) (:pages publish-ir)))
                   nil
                   2)
                  "utf8"))

(defn copy-asset! [from to]
  (ensure-parent! to)
  (.copyFileSync fs from to))

(defn json-literal [value]
  (js/JSON.stringify (clj->js value)))

(defn yaml-line [k v]
  (str k ": " (json-literal v)))

(defn page-frontmatter [page]
  (str/join
   "\n"
   (concat
    ["---"
     (yaml-line "layout" "../../layouts/PostLayout.astro")
     (yaml-line "title" (:title page))]
    (when-let [summary (:summary page)]
      [(yaml-line "summary" summary)])
    (when-let [created-at (:created_at page)]
      [(yaml-line "created_at" created-at)])
    (when-let [published-at (:published_at page)]
      [(yaml-line "published_at" published-at)])
    (when (seq (:tags page))
      [(yaml-line "tags" (:tags page))])
    ["---" ""])))

(defn md->managed-mdx [slug markdown]
  (str/replace markdown
               #"\!\[([^\]]*)\]\(\./([^)]+)\)"
               (fn [[_ alt filename]]
                 (str "![" alt "](/generated-assets/" slug "/" filename ")"))))

(defn emit-post-page! [file page]
  (ensure-parent! file)
  (.writeFileSync fs
                  file
                  (str (page-frontmatter page)
                       "import Mermaid from \"../../components/Mermaid.astro\";\n\n"
                       (md->managed-mdx (:slug page) (:body page)))
                  "utf8"))

(defn emit-assets! [public-root page]
  (doseq [filename (array-seq (.readdirSync fs (:asset-dir page)))
          :when (not= filename "index.md")]
    (copy-asset! (.resolve path (:asset-dir page) filename)
                 (.resolve path public-root (:slug page) filename))))
