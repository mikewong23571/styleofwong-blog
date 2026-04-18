(ns spike.build
  (:require ["fs" :as fs]
            ["path" :as path]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def cwd (.resolve path "."))

(defn abs-path [& segments]
  (.resolve path cwd (apply str (interpose "/" segments))))

(defn read-text [file]
  (.readFileSync fs file "utf8"))

(defn write-text [file content]
  (.mkdirSync fs (.dirname path file) #js {:recursive true})
  (.writeFileSync fs file content "utf8"))

(defn copy-file [from to]
  (.mkdirSync fs (.dirname path to) #js {:recursive true})
  (.copyFileSync fs from to))

(defn parse-frontmatter [raw]
  (let [[_ frontmatter body] (re-matches #"(?s)^---\n(.*?)\n---\n?(.*)$" raw)
        frontmatter-map
        (if frontmatter
          (->> (str/split-lines frontmatter)
               (map #(str/split % #":\s*" 2))
               (reduce (fn [acc [k v]]
                         (assoc acc (keyword k)
                                (cond
                                  (= v "null") nil
                                  (re-matches #"\d{4}-\d{2}-\d{2}" v) v
                                  :else v)))
                       {}))
          {})]
    {:frontmatter frontmatter-map
     :body body}))

(defn source-post-paths [root]
  (->> (.readdirSync fs root #js {:withFileTypes true})
       (array-seq)
       (filter #(.isDirectory %))
       (map #(.-name %))))

(defn discover-posts [source-root]
  (let [posts-root (abs-path source-root "posts")]
    (for [year (source-post-paths posts-root)
          slug (source-post-paths (str posts-root "/" year))]
      {:slug slug
       :year year
       :source-dir (abs-path source-root "posts" year slug)
       :markdown-file (abs-path source-root "posts" year slug "index.md")})))

(defn slug->public-asset [slug filename]
  (str "/generated-assets/" slug "/" filename))

(defn replace-image-paths [slug markdown]
  (str/replace markdown #"\!\[([^\]]*)\]\(\./([^)]+)\)"
               (fn [[_ alt filename]]
                 (str "![" alt "](" (slug->public-asset slug filename) ")"))))

(defn mermaid->component [markdown]
  (loop [remaining markdown
         output ""]
    (if-let [[match code] (re-find #"(?s)```mermaid\n(.*?)```" remaining)]
      (let [idx (.indexOf remaining match)
            before (.slice remaining 0 idx)
            after (.slice remaining (+ idx (count match)))]
        (recur after
               (str output
                    before
                    "\n<Mermaid code={"
                    (pr-str (str/trim code))
                    "} />\n")))
      (str output remaining))))

(defn md->mdx [slug markdown]
  (-> markdown
      (#(replace-image-paths slug %))
      (mermaid->component)))

(defn effective-metadata [metadata slug]
  (let [entry (get metadata slug {})]
    (merge (:llm entry) (:overrides entry))))

(defn frontmatter->mdx [page]
  (str "---\n"
       "layout: ../../layouts/PostLayout.astro\n"
       "title: " (:title page) "\n"
       "summary: " (:summary page) "\n"
       "created_at: " (:created_at page) "\n"
       "status: " (:status page) "\n"
       "tags: [" (str/join ", " (map #(str "\"" % "\"") (:tags page))) "]\n"
       "---\n\n"))

(defn site-manifest [config pages]
  {:site (:site config)
   :astro (:astro config)
   :pages (mapv #(select-keys % [:slug :title :summary :created_at :tags :url]) pages)})

(defn emit-page! [astro-root page]
  (let [target-mdx (abs-path astro-root "src/pages/posts" (str (:slug page) ".mdx"))
        mdx (str (frontmatter->mdx page)
                 "import Mermaid from \"../../components/Mermaid.astro\";\n\n"
                 (:mdx-body page)
                 "\n")]
    (write-text target-mdx mdx)))

(defn emit-assets! [astro-root post]
  (let [files (.readdirSync fs (:source-dir post))
        assets (remove #{"index.md"} (array-seq files))]
    (doseq [filename assets]
      (copy-file (abs-path (:source-dir post) filename)
                 (abs-path astro-root "public/generated-assets" (:slug post) filename)))))

(defn emit-json! [astro-root file data]
  (write-text (abs-path astro-root "src/data" file)
              (js/JSON.stringify (clj->js data) nil 2)))

(defn emit-runtime-config! [astro-root manifest]
  (write-text (abs-path astro-root "managed/runtime-config.json")
              (js/JSON.stringify
               (clj->js
                {:astro (:astro manifest)
                 :site {:theme (name (get-in manifest [:site :theme]))}})
               nil
               2)))

(defn build-page [metadata post]
  (let [{:keys [frontmatter body]} (parse-frontmatter (read-text (:markdown-file post)))
        derived (effective-metadata metadata (:slug post))]
    {:slug (:slug post)
     :title (:title frontmatter)
     :created_at (:created_at frontmatter)
     :status (:status frontmatter)
     :summary (:summary derived)
     :tags (:tags derived)
     :mdx-body (md->mdx (:slug post) body)
     :url (str "/posts/" (:slug post) "/")}))

(defn clean-generated! [astro-root]
  (doseq [dir [(abs-path astro-root "src/pages/posts")
               (abs-path astro-root "src/data")
               (abs-path astro-root "public/generated-assets")]]
    (.rmSync fs dir #js {:recursive true :force true})))

(defn main []
  (let [config (edn/read-string (read-text (abs-path "spikes/content-model/blog.edn")))
        metadata (edn/read-string (read-text (abs-path "spikes/content-model/metadata/posts.edn")))
        astro-root (get-in config [:publish :astro-root])
        posts (discover-posts (get-in config [:content :source-root]))
        pages (mapv #(build-page metadata %) posts)
        manifest (site-manifest config pages)]
    (clean-generated! astro-root)
    (doseq [post posts]
      (emit-assets! astro-root post))
    (doseq [page pages]
      (emit-page! astro-root page))
    (emit-json! astro-root "posts.json" (:pages manifest))
    (emit-json! astro-root "site.json" (select-keys manifest [:site :astro]))
    (emit-runtime-config! astro-root manifest)
    (write-text (abs-path "system/generated/domain-ir.edn") (pr-str pages))
    (write-text (abs-path "system/generated/publish-ir.edn") (pr-str manifest))
    (println "Generated" (count pages) "page(s) into managed Astro runtime.")))
