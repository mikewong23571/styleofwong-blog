(ns blog.test-runner
  (:require [cljs.test :as t]
            [blog.config-test]))

(defn ^:export main []
  (t/run-tests 'blog.config-test))
