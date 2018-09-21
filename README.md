# Special (Conditions)
## Decomplect _when, what, and how._ No mystique! No macros!

[![Clojars Project](https://img.shields.io/clojars/v/special.svg)](https://clojars.org/special)

Special is a **safe** and **easy to use** library that 
allows you to separate the logic of handling 
special conditions from the code in which they arise.

It is specifically **not** exception-handling, but 
integrates with whatever you use for that, including standard try/catch.

* Supports ClojureScript as well as Clojure
* Thread-safe out-of-the-box
* Protects against laziness pitfalls of some other libraries and approaches

## The problem
Most programs contain code where the natural control flow is
obscured because special conditions can arise.

Examples could be
* Missing invalid data
* Data that would cause division by zero
* Run-time exceptions
* Temporarily unavailable resources
* Disk-full

Such "special conditions" are in fact not very special.
Rather, they can be said to be normal.
Yet we often choose to deal with them in crude ways, and not seldom as an afterthought.

This soon creates a patchwork of code that becomes very difficult to reason about.

## The solution provided by this library

| function | input | output
| --- | --- | ---
| `manage` | A function `f` and optional handlers for conditions that might arise during the execution of `f` | A version of `f` where the listed conditions are handled.
| `condition` | A condition keyword `c` , a value and optional handlers for conditions that might arise during the execution of a handler for `c` | The value returned from a matching condition-handler. If no condition handler exists, the :normally handler is used. If that does not exist either, an exception is thrown    

The function `manage` takes a function and zero or more condition handlers.

Handlers are functions of one value. 
They return a new value to be used where the condition was raised.
Instead of a function you can specify a non-function value.

### Step by step introduction

Let's write a naive function than returns n numbers starting from 0:

```clojure
(fn [n]
   (for [i (range n)]
     i))
```

Now let's call it to produce 10 numbers:

```clojure
((fn [n]
   (for [i (range n)]
     i))
  10)
  
=> (0 1 2 3 4 5 6 7 8 9)
```
  
Let's modify the function to return 100 in the place of all odd i's:

```clojure
((fn [n]
   (for [i (range n)]
     (if (odd? i)
       100
       i)))
  10)
=> (0 100 2 100 4 100 6 100 8 100)
```
Now let's express that with conditions.
```clojure
(require '[special.core :refer [condition manage]])
```

```clojure
((fn [n]
   (for [i (range n)]
     (if (odd? i)
       (condition :odd i :normally 100)
       i)))
  10)
=> (0 100 2 100 4 100 6 100 8 100)
```

The call to `condition` here says that the
special condition `:odd` occured for 
some value `i` and that we normally handle 
it by using the value 100.

The handler could be a function instead of 
just a plain value. For instance we could 
choose to normally double all odd numbers:

```clojure
((fn [n]
   (for [i (range n)]
     (if (odd? i)
       (condition :odd i :normally #(* 2 %))
       i)))
  10)
=> (0 2 2 6 4 10 6 14 8 18)
```
Well, actually this means double the condition value. 
You can see that here:
```clojure
=> (0 2 2 6 4 10 6 14 8 18)
((fn [n]
   (for [i (range n)]
     (if (odd? i)
       (condition :odd (- i) :normally #(* 2 %))
       i)))
  10)
=> (0 -2 2 -6 4 -10 6 -14 8 -18)
```
You can pass anything you want as a condition value.

The condition itself must be a keyword.

Normally the condition is handled by the `:normally` handler, 
but it is possible for a function at a higher call level to handle 
the condition itself instead.

The way this is done is by calling the function `manage` with a function and a variable number of condition handlers. `manage` returns a new function, in which these condition handlers are active. 
```clojure
(let [f (fn [n]
          (for [i (range n)]
            (if (odd? i)
              (condition :odd i :normally #(* 2 %))
              i)))
      g (manage f :odd #(+ % 100))] 
  (g 10))
=> (0 101 2 103 4 105 6 107 8 109)
```
Notice how the decision about what to do when 
the `:odd` condition arises in `f` is decomplected 
from `f` and instead taken in `g`.

It does not matter how many calling levels there are 
between `manage`and `condition`.
```clojure
(let [f (fn [n]
          (for [i (range n)]
            (if (odd? i)
              (condition :odd i :normally #(* 2 %))
              i)))
      g #(f (- % 5))
      h (manage g :odd #(+ % 100))]
  (h 10))
=> (0 101 2 103 4)
```
`:normally` is not very special. In fact it is just a 
normal condition handler, but it is special in one way: 
If a condition raised by `condition` is not handled from 
a calling function (ie. by use of `manage`), it will be 
called.

`condition` allows you to specify zero, one or more 
condition handlers that will be active as long as the 
condition is being processed.

If there is no `:normally` handler, a condition can end 
up unhandled. In this case an ex-info exception is thrown. 
```clojure
((fn [n]
   (for [i (range n)]
     (if (odd? i)
       (condition :odd i)
       i)))
  10)

ExceptionInfo Unhandled condition :odd  clojure.core/ex-info (core.clj:4617)
```

As you can see below, a `:normally` handler is not required, 
though it can be good practice to have one.
```clojure
(let [f (fn [n]
          (for [i (range n)]
            (if (odd? i)
              (condition :odd i)
              i)))
      g (manage f :odd 'very-odd)] 
  (g 10))
=> (0 very-odd 2 very-odd 4 very-odd 6 very-odd 8 very-odd)
```

How about some more fun with handlers? Like decomplecting 
the decision about *what* to do about odd numbers 
from *how* to do it.
In the example below, `f` detects *when* we have an `:odd` situation, 
`g` decides *what* to do about it, and `f` also 
decides *how* to do it.

```clojure
(let [f (fn [n]
          (for [i (range n)]
            (if (odd? i)
              (condition :odd i 
                         :unimportant nil
                         :important 'VIP)
              i)))
      g (manage f :odd #(condition 
                         (if (= 7 %) 
                           :important
                           :unimportant)))] 
  (g 10))
=> (0 nil 2 nil 4 nil 6 VIP 8 nil)
```

Or how about managing a condition, then deciding to 
let the called function handle how it `:normally` does?

```clojure
(let [f (fn [n]
          (for [i (range n)]
            (if (odd? i)
              (condition :odd i :normally '...)
              i)))
      g (manage f :odd #(if (= 7 %)
                         'seven
                         (condition :normally)))] 
  (g 10))
=> (0 ... 2 ... 4 ... 6 seven 8 ...)
```
Handlers "know about" each other in a natural way:
```clojure
(let [f (fn [n]
          (for [i (range n)]
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
=> (0 [hello Dolly] 2 [hello Dolly] 4 [hello Dolly] 6 [seven ...] 8 [hello Dolly])
```

## Advanced stuff

Unlike other libraries, **Special** is independent 
of the exception handling of the underlying platform.

Since condition handlers can do anything, including raising 
exceptions, you should be able to do much if not all of the 
stuff you can do with conditional restarts in Lisp, but with 
less of a head-ache.

## A glaring omission?

There is no concept of re-raising a special condition to a
higher level. While not difficult to implement, I feel that
maybe we should wait a while and see if the need is really 
there.

## A note about laziness
 
Because of the way dynamic bindings work in Clojure, it has 
been necessary to make `manage` return an eager function 
instead of a lazy one. Since the eagerfication is achieved
by using `pr-str`, there can be a run-time cost associated 
with this, depending on circumstances.

## Why another condition library?

The main reason I wrote this library is 
that I wanted a minimalistic way to decomplect 
conditions from exceptions.

I wanted it to work in both Clojure and ClojureScript,
and I wanted it to be dependency-free. 

### Alternative libraries

There are other libraries out there that you might want to consider

  Library | Clojure | ClojureScript | Thread-safe | Laziness-safe | Comment 
  --- | --- | --- | --- | --- | ---
  [special](.) | Yes | Yes | Yes | Yes | This library
  [errorkit](https://github.com/richhickey/clojure-contrib/blob/master/src/main/clojure/clojure/contrib/error_kit.clj) | Yes | No | ? | ? | Experimental system by Chris Houser
  [swell](https://github.com/hugoduncan/swell) | Yes | No | ? | ? | A restart library based on [slingshot](https://github.com/scgilardi/slingshot).
  [conditions](https://github.com/bwo/conditions) | Yes | No |  ? | ? |Resumable exceptions library based on [slingshot](https://github.com/scgilardi/slingshot).  
   ribol / [hara.event](https://github.com/zcaudate/hara) | Yes | No |  ? | ? |Part of a much larger library intended to be "a big, monolithic, kitchen-sink type codebase"
 Chris Houser's library-less approach | Yes | No | No | No | See comment below
 [rp.condition](https://github.com/rentpath/rp-condition-clj) | Yes | Yes | No | No | Library that builds upon Chris Houser's approach
 
Chris Houser has given an excellent little talk 
called [_Condition Systems in an Exceptional 
Language_](https://www.youtube.com/watch?v=zp0OEDcAro0)
that you might find interesting to watch. 
However you should be aware that Chris advocates an 
approach that is neither thread-safe nor laziness-safe.
This can lead to unpredictable run-time behaviour,
so I don't recommend the approach in practice.

### Inspirations

The Lisp solution to this problem is called "conditional restarts". 

* Chapter 19 in Peter Siebel's book [Practical Common Lisp](http://www.gigamonkeys.com/book/).
* Kent Pitman [Exceptional Situations In Lisp](http://www.nhplace.com/kent/Papers/Exceptional-Situations-1990.html)
 
## Contributors
 
- [Kurt Heyrman](https://github.com/qrthey)
- [CRHough](https://github.com/CRHough)
- *your name here?*

## License

Copyright © 2016 Mads Olsen.

Distributed under the Eclipse Public License version 1.0
