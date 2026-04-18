(ns blog.publish
  (:require [blog.config :as config]
            [blog.domain :as domain]))

(defn build-publish-ir [config-path content-root metadata-path]
  (let [cfg (config/load-config config-path)
        domain-ir (domain/build-domain-ir config-path content-root metadata-path)]
    {:site (:site cfg)
     :runtime (get-in cfg [:renderer :astro])
     :pages (mapv #(select-keys % [:slug :url :summary :tags :title]) (:posts domain-ir))}))
