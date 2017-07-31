(ns special.eagerize-test
  (:require [clojure.test :refer :all]
            [special.eagerize :refer :all]))

(defn- make-nested-lazy-list
  "Returns a lazy-sequece of e.
   Defaults to random-ints when called with no args."
  ([]
   (make-nested-lazy-list #(rand-int 42)))
  ([e]
   (repeatedly 10 (constantly e))))

(defrecord TestRecord [s])
(deftype TestType [s])

(deftest eagerize-test
  (testing "Can eagerize deep nested Clojure IPersistentList."
    (is (realized?
         (let [ls (list (make-nested-lazy-list))]
           (eagerize ls)
           (first ls)))))
  
  (testing "Can eagerize deep nested Clojure IMapEntry."
    (is (realized?
         (let [ls (first {:e (make-nested-lazy-list)})]
           (eagerize ls)
           (val ls)))))
  
  (testing "Can eagerize deep nested Clojure ISeq."
    (is (realized?
         (let [ls (make-nested-lazy-list (make-nested-lazy-list))]
           (eagerize ls)
           (first ls)))))
  
  (testing "Can eagerize deep nested Clojure IRecord."
    (is (realized?
         (let [ls (->TestRecord (make-nested-lazy-list))]
           (eagerize ls)
           (:s ls)))))
  
  (testing "Can eagerize deep nested Clojure IType."
    (is (realized?
         (let [ls (TestType. (make-nested-lazy-list))]
           (eagerize ls)
           (.-s ls)))))
  
  (testing "Can eagerize deep nested Java Iterable."
    (is (realized?
         (let [ls (doto (java.util.LinkedList.)
                        (.add (make-nested-lazy-list)))]
           (eagerize ls)
           (first ls)))))
  
  (testing "Can eagerize deep nested Java AbstractMap."
    (is (realized?
         (let [ls (doto (java.util.HashMap.)
                        (.put "a" (make-nested-lazy-list)))]
           (eagerize ls)
           (.get ls "a")))))
  
  (testing "Can eagerize deep nested Java Stack."
    (is (realized?
         (let [ls (doto (java.util.Stack.)
                        (.push (make-nested-lazy-list)))]
           (eagerize ls)
           (.pop ls)))))
  
  (testing "Can eagerize deep nested Java Arrays."
    (is (realized?
         (let [ls (doto (make-array clojure.lang.ISeq 2)
                        (aset 0 (make-nested-lazy-list)))]
           (eagerize ls)
           (aget ls 0))))))
