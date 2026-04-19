(ns blog.governance.rules)

(defn required-field-errors [publish-ir]
  (->> (:pages publish-ir)
       (mapcat (fn [page]
                 (concat
                  (when-not (:title page)
                    [{:level :error
                      :rule :missing-title
                      :page (:slug page)
                      :message "Missing title"}])
                  (when-not (:created_at page)
                    [{:level :error
                      :rule :missing-created-at
                      :page (:slug page)
                      :message "Missing created_at"}])
                  (when-not (:status page)
                    [{:level :error
                      :rule :missing-status
                      :page (:slug page)
                      :message "Missing status"}]))))
       vec))

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

(defn lifecycle-errors [publish-ir]
  (->> (:pages publish-ir)
       (mapcat (fn [page]
                 (concat
                  (when (and (= "published" (:status page))
                             (not (:published_at page)))
                    [{:level :error
                      :rule :published-missing-published-at
                      :page (:slug page)
                      :message "Published content must include published_at"}])
                  (when (and (= "draft" (:status page))
                             (:published_at page))
                    [{:level :error
                      :rule :draft-has-published-at
                      :page (:slug page)
                      :message "Draft content must not include published_at"}]))))
       vec))

(defn duplicate-slug-errors [publish-ir]
  (->> (:pages publish-ir)
       (group-by :slug)
       vals
       (filter #(> (count %) 1))
       (mapcat (fn [pages]
                 (for [page pages]
                   {:level :error
                    :rule :duplicate-slug
                    :page (:slug page)
                    :message (str "Duplicate slug: " (:slug page))})))
       vec))

(defn duplicate-url-errors [publish-ir]
  (->> (:pages publish-ir)
       (group-by :url)
       vals
       (filter (fn [pages]
                 (and (> (count pages) 1)
                      (> (count (distinct (map :slug pages))) 1))))
       (mapcat (fn [pages]
                 (for [page pages]
                   {:level :error
                    :rule :duplicate-url
                    :page (:slug page)
                    :message (str "Duplicate url: " (:url page))})))
       vec))

(defn metadata-override-errors [publish-ir]
  (->> (:pages publish-ir)
       (mapcat (fn [page]
                 (let [metadata-entry (:metadata-entry page)
                       overrides (get-in page [:metadata-entry :overrides])]
                   (when (and metadata-entry
                              (or (nil? overrides)
                                  (nil? (:tags overrides))
                                  (nil? (:summary overrides))))
                     [{:level :error
                       :rule :missing-metadata-overrides
                       :page (:slug page)
                       :message "Metadata overrides must include tags and summary"}]))))
       vec))

(defn all-errors [publish-ir policy]
  (vec
   (concat
    (required-field-errors publish-ir)
    (lifecycle-errors publish-ir)
    (duplicate-slug-errors publish-ir)
    (duplicate-url-errors publish-ir)
    (metadata-override-errors publish-ir)
    (unknown-tag-errors publish-ir policy))))
