(ns blog.publish-test
  (:require [cljs.test :refer [deftest is]]
            [blog.publish :as publish]))

(deftest emits-publish-ir-and-managed-runtime-config
  (let [result (publish/build-publish-ir "system/config/blog.edn" "content" "metadata/posts.edn")]
    (is (= "Content-First Blog" (get-in result [:site :title])))
    (is (= "/posts/hello-world/" (get-in result [:pages 0 :url])))
    (is (= true (get-in result [:runtime :features :mermaid])))))
