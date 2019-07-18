(ns specify-it.bst-spec
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :as test]
            [specify-it.bst :as bst]
            [specify-it.bug2 :as bug1]
            [specify-it.bug2 :as bug2]
            [specify-it.bug3 :as bug3]
            [specify-it.bug3 :as bug4]
            [specify-it.bug3 :as bug5]
            [clojure.pprint :as pprint]
            [specify-it.bst-common :as common]))

(def insert' bug5/insert')
(def nil' bug5/nil')
(def valid' bug5/valid')
(def find' bug5/find')
(def delete' bug5/delete')
(def union' bug5/union')

(def to-sorted-list' common/to-sorted-list')
(def insertions common/insertions)

(defn equiv [t1 t2] (= (to-sorted-list' t1) (to-sorted-list' t2)))

(defn prop-nil-valid [] (valid' (nil')))
(defn prop-insert-valid [k v t] (valid' (insert' k v t)))
(defn prop-delete-valid [k t] (valid' (delete' k t)))
(defn prop-union-valid [t t'] (valid' (union' t t')))
(defn prop-arbitrary-valid [t] (valid' t))

(defn prop-insert-post [k v t k']
  (= (find' k' (insert' k v t))
     (if (= k k')
       v
       (find' k' t))))

(defn prop-insert-post-same-key [k v t]
  (prop-insert-post k v t k))

(defn prop-union-post [t t' k]
  (let [r (find' k (union' t t'))]
    (or
      (= (find' k t) r)
      (= (find' k t') r))))

(defn prop-find-post-present [k v t]
  (= v
     (find' k (insert' k v t))))

(defn prop-find-post-absent [k t]
  (= :not-found
     (find' k (delete' k t))))

(defn prop-insert-delete-complete [k t]
  (if (= :not-found (find' k t))
    (= t (delete' k t))
    (= t (insert' k (find' k t) t))))

(defn prop-insert-insert [k v k' v' t]
  (equiv
    (insert' k v (insert' k' v' t))
    (if (= k k')
      (insert' k v t)
      (insert' k' v' (insert' k v t)))))

(defn prop-insert-insert-weak [k v k' v' t]
  (or (= k k')
      (equiv (insert' k v (insert' k' v' t)) (insert' k' v' (insert' k v t)))))

(defn prop-insert-delete [k v k' t]
  (equiv
    (insert' k v (delete' k' t))
    (if (= k k')
      (insert' k v t)
      (delete' k' (insert' k v t)))))

(defn prop-insert-union [k v t t']
  (equiv
    (insert' k v (union' t t'))
    (union' (insert' k v t) t')))

(defn prop-insert-preserves-equiv [k v equiv-t equiv-t']
  (equiv
    (insert' k v equiv-t)
    (insert' k v equiv-t')))

(defn prop-delete-preserves-equiv [k equiv-t equiv-t']
  (equiv
    (delete' k equiv-t)
    (delete' k equiv-t')))

(defn prop-union-preserves-equiv [equiv-t equiv-t']
  (equiv
    (union' equiv-t equiv-t')
    (union' equiv-t' equiv-t)))

(defn prop-find-preserves-equiv [k equiv-t equiv-t']
  (=
    (find' k equiv-t)
    (find' k equiv-t')))

(defn prop-union-nil [t]
  (= t (union' (nil') t)))

(defn prop-union-insert [t t' k v]
  (equiv
    (union' (insert' k v t) t')
    (insert' k v (union' t t'))))

(defn prop-insert-complete [t]
  (= t
     (reduce (fn [t [k v]] (insert' k v t)) (nil') (insertions t))))

(defn prop-insert-complete-for-delete [k t]
  (prop-insert-complete (delete' k t)))

(defn prop-insert-complete-for-union [t t']
  (prop-insert-complete (union' t t')))

(defn prop-nil-model []
  (= (to-sorted-list' (nil')) []))

(defn prop-insert-model [k v t]
  (=
    (to-sorted-list' (insert' k v t))
    (into [] (assoc (dissoc (into (sorted-map) (insertions t)) k) k v))))

(defn prop-delete-model [k t]
  (=
    (to-sorted-list' (delete' k t))
    (into [] (dissoc (into (sorted-map) (insertions t)) k))))

(defn prop-union-model [t t']
  (to-sorted-list' (union' t t'))
  (into [] (merge (into (sorted-map) (insertions t)) (into (sorted-map) (insertions t')))))

(defn prop-find-model [k t]
  (=
    (find' k t)
    (get (into (sorted-map) (insertions t)) k :not-found)))

(def key-gen (gen/scale #(/ % 2) gen/large-integer))

(def tree-gen
  (gen/fmap
    (fn [kvs]
      (reduce (fn [t [k v]] (insert' k v t)) (nil') kvs))
    (gen/vector (gen/tuple key-gen gen/large-integer))))

(def equiv-tree-gen (gen/such-that #(equiv (first %) (second %))
                                   (gen/fmap
                                     (fn [kvs]
                                       [
                                        (reduce (fn [t [k v]] (insert' k v t)) (nil') kvs)
                                        (reduce (fn [t [k v]] (insert' k v t)) (nil') (shuffle kvs))])
                                     (gen/vector (gen/tuple key-gen gen/large-integer)))))


(def props-with-bound-generators
  {
   :prop-nil-valid                  (prop/for-all* [] prop-nil-valid)
   :prop-insert-valid               (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen]
                                                  (prop-insert-valid k v t))
   :prop-delete-valid               (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-delete-valid k t))
   :prop-union-valid                (prop/for-all [t1 tree-gen
                                                   t2 tree-gen]
                                                  (prop-union-valid t1 t2))
   :prop-arbitrary-valid            (prop/for-all [t tree-gen]
                                                  (prop-arbitrary-valid t))
   :prop-insert-post                (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen
                                                   k' gen/large-integer]
                                                  (prop-insert-post k v t k'))
   :prop-insert-post-same-key       (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen]
                                                  (prop-insert-post-same-key k v t))
   :prop-union-post                 (prop/for-all [t tree-gen
                                                   t' tree-gen
                                                   k key-gen]
                                                  (prop-union-post t t' k))
   :prop-find-post-present          (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen]
                                                  (prop-find-post-present k v t))
   :prop-find-post-absent           (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-find-post-absent k t))
   :prop-insert-delete-complete     (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-insert-delete-complete k t))
   :prop-insert-insert              (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   k' key-gen
                                                   v' gen/large-integer
                                                   t tree-gen]
                                                  (prop-insert-insert k v k' v' t))
   :prop-insert-insert-weak         (prop/for-all [[k k'] (gen/such-that #(not= (first %) (second %)) (gen/tuple key-gen key-gen))
                                                   v gen/large-integer
                                                   v' gen/large-integer
                                                   t tree-gen]
                                                  (prop-insert-insert-weak k v k' v' t))
   :prop-insert-delete              (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   k' key-gen
                                                   t tree-gen]
                                                  (prop-insert-delete k v k' t))
   :prop-insert-union               (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen
                                                   t' tree-gen]
                                                  (prop-insert-union k v t t'))
   :prop-insert-preserves-equiv     (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   [equiv-t equiv-t'] equiv-tree-gen]
                                                  (prop-insert-preserves-equiv k v equiv-t equiv-t'))
   :prop-delete-preserves-equiv     (prop/for-all [k key-gen
                                                   [equiv-t equiv-t'] equiv-tree-gen]
                                                  (prop-delete-preserves-equiv k equiv-t equiv-t'))
   :prop-union-preserves-equiv      (prop/for-all [[equiv-t equiv-t'] equiv-tree-gen]
                                                  (prop-union-preserves-equiv equiv-t equiv-t'))
   :prop-find-preserves-equiv       (prop/for-all [k gen/large-integer
                                                   [equiv-t equiv-t'] equiv-tree-gen]
                                                  (prop-find-preserves-equiv k equiv-t equiv-t'))
   :prop-equivs                     (prop/for-all [[equiv-t equiv-t'] equiv-tree-gen]
                                                  (equiv equiv-t equiv-t'))
   :prop-union-nil                  (prop/for-all [t tree-gen]
                                                  (prop-union-nil t))
   :prop-union-insert               (prop/for-all [t tree-gen
                                                   t' tree-gen
                                                   k key-gen
                                                   v gen/large-integer]
                                                  (prop-union-insert t t' k v))
   :prop-insert-complete            (prop/for-all [t tree-gen]
                                                  (prop-insert-complete t))
   :prop-insert-complete-for-delete (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-insert-complete-for-delete k t))
   :prop-insert-complete-for-union  (prop/for-all [t tree-gen
                                                   t' tree-gen]
                                                  (prop-insert-complete-for-union t t'))
   :prop-nil-model                  (prop/for-all [] (prop-nil-model))
   :prop-insert-model               (prop/for-all [k key-gen
                                                   v gen/large-integer
                                                   t tree-gen]
                                                  (prop-insert-model k v t))
   :prop-delete-model               (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-delete-model k t))
   :prop-union-model                (prop/for-all [t tree-gen
                                                   t' tree-gen]
                                                  (prop-union-model t t'))
   :prop-find-model                 (prop/for-all [k key-gen
                                                   t tree-gen]
                                                  (prop-find-model k t))})

(defn check-props []
  (doseq [[prop-name prop] props-with-bound-generators]
    (let [result (tc/quick-check 100 prop)]
      (if (:pass? result)
        (println {:prop-name prop-name :pass? true})
        (pprint/pprint [prop-name result])))))

(check-props)

