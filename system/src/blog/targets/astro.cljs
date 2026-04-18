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
                   (clj->js (mapv #(select-keys % [:slug :url :summary :tags :title]) (:pages publish-ir)))
                   nil
                   2)
                  "utf8"))

(defn copy-asset! [from to]
  (ensure-parent! to)
  (.copyFileSync fs from to))

(defn md->managed-mdx [slug markdown]
  (-> markdown
      (str/replace #"(?s)```mermaid\n(.*?)```"
                   (fn [[_ code]]
                     (str "\n<Mermaid code={" (pr-str (str/trim code)) "} />\n")))
      (str/replace #"\!\[([^\]]*)\]\(\./([^)]+)\)"
                   (fn [[_ alt filename]]
                     (str "![" alt "](/generated-assets/" slug "/" filename ")")))))

(defn emit-post-page! [file page]
  (ensure-parent! file)
  (.writeFileSync fs
                  file
                  (str "---\n"
                       "layout: ../../layouts/PostLayout.astro\n"
                       "title: " (:title page) "\n"
                       "---\n\n"
                       "import Mermaid from \"../../components/Mermaid.astro\";\n\n"
                       (md->managed-mdx (:slug page) (:body page)))
                  "utf8"))

(defn emit-assets! [public-root page]
  (doseq [filename (array-seq (.readdirSync fs (:asset-dir page)))
          :when (not= filename "index.md")]
    (copy-asset! (.resolve path (:asset-dir page) filename)
                 (.resolve path public-root (:slug page) filename))))
