(ns blog.test-runner
  (:require [cljs.test :as t]
            [blog.config-test]
            [blog.source-test]
            [blog.domain-test]))

(defn ^:export main []
  (t/run-tests 'blog.config-test 'blog.source-test 'blog.domain-test))
