(ns CatchUp.filters)

;; matching user with itmes.
(defn keyword-match?
  "If content has keyword, return true. Or else return false. Match will be case insensitive."
  [keyword content]
  (if (and (not (nil? keyword))
           (not (nil? content)))
    (if (> (.indexOf (.toLowerCase content) (.toLowerCase keyword))
           -1)
      true
      false)
    false))

;; FIXME: change or to and, we want match all keywords
(defn keywords-match?
  [keywords content]
  (some #(keyword-match? %1 content) keywords))

(defn all-keywords-match?
  [keywords content]
  (every? #(keyword-match? %1 content) keywords))

(defn filter-items-by-keywords
  [keywords items]
  (filter #(keywords-match?
            keywords
            (:description %1))
          items))

(defn filters-match?
  [filters content]
  (some #(keywords-match? (val %1) content) filters))

(defn user-filter-match?
  [user item]
  ;; (pprint/pprint user)
  ;; (pprint/pprint item)
  (println "user-filter-match?" (:id (:user user)) (:_id item))
  (filters-match? (get-in user [:subscriptions (keyword (:source item)) :filters])
                  (:description item)))

(defn filter-expired-items
  [items]
  (filter #(not (:expired? %1)) items))
