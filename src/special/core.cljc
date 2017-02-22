(ns special.core)

(defonce ^:dynamic *-special-condition-handlers-* {})

(defn- manage-with-courage
  "Takes an eager function f and an \"inlined\" map of conditions to handlers.
  Returns a function in which these conditions are managed.

  The returned function can safely be called from another thread
  than the one in which it was created.

  CAVEAT:
  It is very important that f returns a result that has been
  realized for all parts in which special conditions can arise.

  As long as f returns a fully realized result, all is good.

  Any conditions raised lazily after f returns will result in
  unexpected bahaviour: Either they will be unmanaged, or
  they will be managed by whatever handlers are at the time
  existing in the dynamic thread execution context."
  [f & {:as restarts}]
  (fn [& args]
    (binding [*-special-condition-handlers-* (merge *-special-condition-handlers-* restarts)]
      (apply f args))))

(defn- eagerize
  "Turns a lazy function into an eager function, at the
  run-time cost of using pr-str to fully realize the
  function result."
  [f]
  (fn [& args]
    (let [res (apply f args)
          _ (pr-str res)]
      res)))

(defn manage
  "Takes a function f and an \"inlined\" map of conditions and keywords.
  Returns a function in which these conditions are managed.

  The returned function can safely be called from another thread
  than the one in which it was created.

  f is allowed to be lazy, but the result must be finite, as it will
  always be fully realized. In other words: manage returns an eager function."

  [f & restarts]
  (apply manage-with-courage (eagerize f) restarts))

(defn condition
  "Raise a condition c with optional value v and optionally an \"inlined\" map of conditions to handlers.
  Unmanaged conditions will throw an exception of type ex-info, unless an :normally handler is defined locally.
  c must be a keyword."
  [condition & [value & {:as handlers}]]
  (assert (and (keyword? condition) (not (#{:trace} condition))))
  (let [get-h (fn [m k] (if (contains? m k)
                          (let [h (m k)]
                            (case h
                              nil ::nil
                              false ::false
                              h))))
        x (or (get-h *-special-condition-handlers-* condition)
              (get-h handlers :normally)
              (throw (ex-info (str "Unhandled condition " condition)
                              {::condition condition
                               :value      value})))]
    (cond (fn? x) ((if (seq handlers)
                     (apply manage x (apply concat handlers))
                     x)
                    value)
          (= ::nil x) nil
          (= ::false x) false
          :else x)))
