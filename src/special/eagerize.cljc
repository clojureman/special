(ns special.eagerize)

#?(:clj
   (defonce array-object-type (delay (Class/forName "[Ljava.lang.Object;"))))

(defprotocol Eagerizable
  "Container types that want to work safely with special should implement this
   protocol in a way where all elements they contain, even deeply nested, will
   be realized and made eager."
  (eagerize [this]))

#?(:clj
   (extend-protocol Eagerizable
     nil
     (eagerize [this] this)
     java.lang.Object
     (eagerize [this] (if (instance? @array-object-type this)
                        (doall (map eagerize this))
                        this))
     clojure.lang.IPersistentList
     (eagerize [this] (apply list (map eagerize this)))
     clojure.lang.IMapEntry
     (eagerize [this] (vec (map eagerize this)))
     clojure.lang.ISeq
     (eagerize [this] (doall (map eagerize this)))
     clojure.lang.IRecord
     (eagerize [this] (reduce (fn [r x] (conj r (eagerize x))) this this))
     clojure.lang.IPersistentCollection
     (eagerize [this] (into (empty this) (map eagerize this)))
     clojure.lang.IType
     (eagerize [this] (doall
                       (map #(eagerize (.get % this))
                            (.getFields (class this)))))
     clojure.lang.Delay
     (eagerize [this] (eagerize (deref this)))
     java.lang.Iterable
     (eagerize [this] (doall (map eagerize this)))
     java.util.Map
     (eagerize [this] (doall (map eagerize (.values this))))))

#?(:cljs (extend-protocol Eagerizable
           default
           (eagerize [this] (pr-str this))))
