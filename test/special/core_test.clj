(ns special.core-test
  (:require [clojure.test :refer :all]
            [special.core :refer :all]))

(deftest hello-dolly
  (testing "Hello Dolly"
    (is (=
          (let [f (fn [n]
                    (for [i (range 10)]
                      (if (odd? i)
                        (condition :odd i
                                   :normally '...
                                   :abnormally #(vector % (condition :normally)))
                        i)))
                g (manage f
                          :hello #(vector 'hello %)
                          :odd #(if (= 7 %)
                                 (condition :abnormally 'seven)
                                 (condition :hello 'Dolly)))]
            (g 10))
          '(0 [hello Dolly] 2 [hello Dolly] 4 [hello Dolly] 6 [seven ...] 8 [hello Dolly])))))

(deftest handlers
  (testing "non-function handlers"
    (is (= nil
           (condition :yxz 123 :normally nil)))
    (is (= false
           (condition :yxz 123 :normally false)))
    (is (= 999
           (condition :yxz 123 :normally 999)))))