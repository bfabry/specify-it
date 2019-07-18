(ns specify-it.bug3
  (:require [specify-it.bst-common :as common]))

(defn find' [k-new t]
  (if (= t :leaf)
    :not-found
    (let [{:keys [l r k v]} t]
      (cond
        (= k-new k) v
        (< k-new k) (recur k-new l)
        :else (recur k-new r)))))

(defn nil' [] :leaf)

(defn insert' [k-new v t]
  (if (= t :leaf)
    {:l :leaf :r :leaf :k k-new :v v}
    (let [{:keys [l r k]} t]
      (when (= nil k)
        (println t))
      (cond
        (= k-new k) t
        (< k-new k) (assoc t :l (insert' k-new v l))
        :else (assoc t :r (insert' k-new v r))))))


(defn union' [t1 t2]
  (reduce (fn [t [k v]] (insert' k v t)) t2 (common/insertions t1)))

(defn delete' [k-new t]
  (if (= t :leaf)
    :leaf
    (let [{:keys [l r k]} t]
      (cond
        (= k-new k) (union' l r)
        (< k-new k) (assoc t :l (delete' k-new l))
        :else (assoc t :r (delete' k-new r))))))

(defn keys' [t]
  (map first (common/insertions t)))

(defn valid' [t]
  (if (= :leaf t)
    true
    (let [{:keys [l r k v]} t]
      (and
        (valid' l)
        (valid' r)
        (every? #(< % k) (keys' l))
        (every? #(> % k) (keys' r))))))
