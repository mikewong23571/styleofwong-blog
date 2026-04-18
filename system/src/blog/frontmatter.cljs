(ns blog.frontmatter
  (:require [clojure.string :as str]))

(defn parse-frontmatter [raw]
  (let [[_ frontmatter body] (re-matches #"(?s)^---\n(.*?)\n---\n?(.*)$" raw)
        kvs (->> (str/split-lines frontmatter)
                 (map #(str/split % #":\s*" 2))
                 (into {}))]
    {:frontmatter {:title (get kvs "title")
                   :created_at (get kvs "created_at")
                   :status (get kvs "status")}
     :body body}))
