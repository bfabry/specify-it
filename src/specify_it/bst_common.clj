(ns specify-it.bst-common)


(defn insertions [t]
  (if (= t :leaf)
    []
    (let [{:keys [l r k v]} t]
      (into [[k v]] (into (insertions l) (insertions r))))))

(defn to-sorted-list' [t]
  (sort (insertions t)))
