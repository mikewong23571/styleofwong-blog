(ns blog.config-test
  (:require [cljs.test :refer [deftest is]]
            [blog.config :as config]))

(deftest loads-runtime-matrix-from-system-config
  (let [cfg (config/load-config "system/config/blog.edn")]
    (is (= "astro" (get-in cfg [:renderer :kind])))
    (is (= "6.1.8" (get-in cfg [:renderer :astro :version])))
    (is (= true (get-in cfg [:renderer :astro :features :math])))
    (is (= "Mike Wong" (get-in cfg [:site :hero :eyebrow])))
    (is (= ["X" "GitHub" "Email"]
           (mapv :label (get-in cfg [:site :hero :links]))))))
