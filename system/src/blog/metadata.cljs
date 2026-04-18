(ns blog.metadata
  (:require ["fs" :as fs]
            [clojure.edn :as edn]))

(defn load-metadata [path]
  (-> (.readFileSync fs path "utf8")
      (edn/read-string)))

(defn effective-entry [metadata slug]
  (let [entry (get metadata slug)]
    (merge (:llm entry) (:overrides entry))))

(defn entry [metadata slug]
  (get metadata slug))
