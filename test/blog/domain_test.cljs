(ns blog.domain-test
  (:require [cljs.test :refer [deftest is]]
            [blog.domain :as domain]))

(deftest normalizes-markdown-and-metadata-into-domain-ir
  (let [ir (domain/build-domain-ir "system/config/blog.edn" "content" "metadata/posts.edn")
        post (first (:posts ir))]
    (is (= "hello-world" (:slug post)))
    (is (= "Hello Content System" (:title post)))
    (is (= "/posts/hello-world/" (:url post)))
    (is (= ["clojurescript" "astro" "content-system"] (:tags post)))
    (is (= "published" (:status post)))))
