(ns blog.governance.core
  (:require ["fs" :as fs]
            [blog.governance.rules :as rules]))

(defn run-governance [publish-ir policy]
  (let [errors (rules/all-errors publish-ir policy)
        report {:errors errors
                :warnings []
                :ok? (empty? errors)}]
    (.mkdirSync fs "system/reports" #js {:recursive true})
    (.writeFileSync fs
                    "system/reports/governance-report.json"
                    (js/JSON.stringify (clj->js report) nil 2)
                    "utf8")
    report))
