(ns specify-it.reverse
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(defn test-reverse []
  (= (reverse [1 2 3]) [3 2 1]))

(test-reverse)
;; true

(comment
  (def prop-reverse
    (prop/for-all [xs (gen/vector gen/small-integer)]
                  (= (reverse xs) ??))))

(comment
  (def prop-reverse
    (prop/for-all [xs (gen/vector gen/small-integer)]
                  (= (reverse xs) (predict-rev xs)))))

(def prop-reverse
  (prop/for-all [xs (gen/vector gen/large-integer)]
                (= (reverse (reverse xs)) xs)))

(tc/quick-check 100 prop-reverse)
;; {:result true, :pass? true, :num-tests 100, :time-elapsed-ms 18, :seed 1563381856511}

(defn reverse' [xs]
  xs)

(def prop-reverse'
  (prop/for-all [xs (gen/vector gen/large-integer)]
                (= (reverse' (reverse' xs)) xs)))

(tc/quick-check 100 prop-reverse')
;; {:result true, :pass? true, :num-tests 100, :time-elapsed-ms 17, :seed 1563381877360}

(def prop-wrong
  (prop/for-all [xs (gen/vector gen/large-integer)]
                (= (reverse xs) xs)))

(tc/quick-check 100 prop-wrong)
;; {:shrunk {:total-nodes-visited 7, :depth 1, :pass? false, :result false, :result-data nil, :time-shrinking-ms 1, :smallest [[0 -1]]}, :failed-after-ms 0, :num-tests 3, :seed 1563381885459, :fail [[-2 -1]], :result false, :result-data nil, :failing-size 2, :pass? false}
