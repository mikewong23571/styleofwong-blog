(ns blog.targets.astro
  (:require ["fs" :as fs]
            ["path" :as path]))

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
  (.writeFileSync fs file (js/JSON.stringify (clj->js (:pages publish-ir)) nil 2) "utf8"))

(defn emit-post-page! [file]
  (ensure-parent! file)
  (.writeFileSync fs
                  file
                  (str "---\n"
                       "layout: ../../layouts/PostLayout.astro\n"
                       "title: Hello Content System\n"
                       "---\n\n"
                       "Hello managed page.\n")
                  "utf8"))
