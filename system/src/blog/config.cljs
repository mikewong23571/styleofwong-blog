(ns blog.config
  (:require ["fs" :as fs]
            [clojure.edn :as edn]))

(defn load-config [path]
  (-> (.readFileSync fs path "utf8")
      (edn/read-string)))
