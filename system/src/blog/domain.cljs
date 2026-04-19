(ns blog.domain
  (:require ["fs" :as fs]
            [blog.frontmatter :as frontmatter]
            [blog.metadata :as metadata]
            [blog.source :as source]))

(defn build-domain-ir [_config-path content-root metadata-path]
  (let [posts (source/discover-posts (str content-root "/posts"))
        metadata-map (metadata/load-metadata metadata-path)]
    {:posts
     (mapv (fn [post]
             (let [{:keys [frontmatter body]} (frontmatter/parse-frontmatter
                                              (.readFileSync fs (:markdown-path post) "utf8"))
                   metadata-entry (metadata/entry metadata-map (:slug post))]
               (merge post
                      frontmatter
                      {:body body}
                      {:url (str "/posts/" (:slug post) "/")}
                      {:metadata-entry metadata-entry}
                      (metadata/effective-entry metadata-map (:slug post)))))
           posts)}))
