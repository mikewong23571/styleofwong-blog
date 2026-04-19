(ns blog.source
  (:require ["fs" :as fs]
            ["path" :as path]))

(defn directory? [location]
  (.isDirectory (.statSync fs location)))

(defn subdirectories [root]
  (->> (.readdirSync fs root #js {:withFileTypes true})
       (array-seq)
       (filter #(.isDirectory %))
       (map #(.join path root (.-name %)))
       (sort)))

(defn index-file [dir]
  (.join path dir "index.md"))

(defn post-dir->entry [year-dir post-dir]
  (let [markdown-path (index-file post-dir)]
    (when (.existsSync fs markdown-path)
      {:slug (.basename path post-dir)
       :bucket-year (.basename path year-dir)
       :markdown-path markdown-path
       :asset-dir post-dir})))

(defn discover-posts [root]
  (->> (subdirectories root)
       (mapcat (fn [year-dir]
                 (when (directory? year-dir)
                   (->> (subdirectories year-dir)
                        (map #(post-dir->entry year-dir %))))))
       (keep identity)
       (sort-by :markdown-path)
       vec))
