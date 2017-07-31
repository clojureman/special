(ns special.eagerize)

#?(:clj (defn eagerize
          "Recursively iterate over Clojure data structures and Java data structures,
           evaluating each element, so as to force eagerization of any lazy construct.
           Supports:
           - Clojure IPersistentList, IMapEntry, ISeq, IRecord, IPersistentCollection
             and IType.
           - Java Iterable, Map and Arrays."
          [form]
          (cond
            (nil? form) form
            (list? form) (apply list (map eagerize form))
            (instance? clojure.lang.IMapEntry form) (vec (map eagerize form))
            (seq? form) (doall (map eagerize form))
            (instance? clojure.lang.IRecord form)
            (reduce (fn [r x] (conj r (eagerize x))) form form)
            (coll? form) (into (empty form) (map eagerize form))
            (instance? java.lang.Iterable form) (doall (map eagerize form))
            (instance? java.util.Map form) (doall (map eagerize (.values form)))
            (instance? clojure.lang.IType form) (doall
                                                 (map #(eagerize (.get % form))
                                                      (.getFields (class form))))
            (.isArray (type form)) (doall (map eagerize form))
            :else form)
          nil))

#?(:cljs (defn eagerize
           "Relies on each form's implementation of .toString or print-method
            to recursively visit all element of form, so as to eagerize form.
            Will fail to eagerize if print-method or .toString is not implemented
            in a way that visit all elements at all levels.
            Pays the extra cost both in CPU and memory of printing."
           [form]
           (pr-str form)))
