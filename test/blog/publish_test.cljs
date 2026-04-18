(ns blog.publish-test
  (:require [cljs.test :refer [deftest is]]
            [blog.publish :as publish]))

(deftest emits-publish-ir-and-managed-runtime-config
  (let [result (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")]
    (is (= "Mike Wong" (get-in result [:site :title])))
    (is (= "Notes by Mike Wong" (get-in result [:site :hero :headline])))
    (is (= "https://x.com/0xMikeWong"
           (get-in result [:site :hero :links 0 :href])))
    (is (= "https://github.com/mikewong23571"
           (get-in result [:site :hero :links 1 :href])))
    (is (= "/posts/hello-world/" (get-in result [:pages 0 :url])))
    (is (= true (get-in result [:runtime :features :mermaid])))))
