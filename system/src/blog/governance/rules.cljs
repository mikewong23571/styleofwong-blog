(ns blog.governance.rules)

(defn unknown-tag-errors [publish-ir {:keys [tags]}]
  (->> (:pages publish-ir)
       (mapcat (fn [page]
                 (for [tag (:tags page)
                       :when (not (contains? tags tag))]
                   {:level :error
                    :rule :unknown-tag
                    :page (:slug page)
                    :message (str "Unknown tag: " tag)})))
       vec))
