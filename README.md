# The opinionated guide to Java 8+ Futures

This is intended as quick guide to getting started with futures in Java.
It includes some theory, some best practices, exercise material and known issues to keep in mind.
Hopefully all of this can be useful to get a better understanding of how futures work in Java 8+.

# Introduction

Let us start with defining what a future is.

A future is a container for a value, similar to an `AtomicReference<T>`.
It has two fundamental states - incomplete (a value has not been set yet) and complete (a specific value has been set).

The purpose of a future is to enable asynchronous programming. Instead of blocking a thread, waiting for
some expensive or time-consuming operation to complete, you can finish immediately and defer the next step
of the process until the future is complete.

## Producers and consumers

You can think of futures from two sides - producers and consumers.

We usually spend most of the time using futures as consumers, and then we usually seem them as read-only containers
that change state once. We can subscribe to be notified when the state changes and trigger further work.

From a producer side, we should instead use the term Promise. You create a _promise_ that you will produce a value
and some time later when the value is ready, you complete or fulfill the promise. The future associated with the promise
will thus complete and the consumer can act on it.

You can think of it as the following flow:
* The producer creates a Promise
* The produces extracts the Future from the Promise and gives the Future to the consumer.
* The consumer starts registering callbacks and transformations on the future.
* At some point later the producer fulfills the Promise and the callbacks will trigger for the consumer. 
 
(There is a good [wikipedia article](https://en.wikipedia.org/wiki/Futures_and_promises) about this
if you want to read more about it)

## Futures as monads

You can think of futures in Java as monads, similar to the ´Optional` class.
It's a value container that you can apply transformations on to get a different container with a different value.

## Futures as immutables

If you look at futures from the consumer side and focus on the transformation operations, you can see them
as immutable. The futures themselves don't change, they just contain a value. From the consumer side,
it doesn't matter if the futures are complete or not, they will eventually apply the transformations.

This also means that you shouldn't mix futures with mutable objects -
that introduces risks of race condition related bugs!  

> **_Experiment:_** [MutableTest](src/test/java/se/krka/futures/MutableTest.java)

# A brief history of futures in Java

Java 1.5 introduced the `Future` interface but it was a very limited - you could call `get()` and `isDone()`
but there was no way to get notified of state changes. Instead you would need to do polling.

Google Guava introduced the `ListenableFuture` interface which extends `Future` but also adds
`addListener(Runnable, executor)` - this one method enabled a more asynchronous development model.
All other useful methods could now be implemented as utility functions on top of the primitives.

The Google Guava class `Futures` included some very useful methods:
* `addCallback(callback)` - convenience method on top of `addListener`
* `transform(function)` - return a new future after applying a function to the values
* `transformAsync(function)` - like transform, but the function should return a future instead.
  (Similar to Java 8 `thenCompose`)

Then with Java 8 we got `CompletableFuture` which had achieved feature parity with Google Guava futures,
though with differences in:
* Fluent API - `CompletableFuture` has a fluent API unlike `ListenableFuture` (but Google later added `FluentFuture` too)
* Mutability - Google futures separates the producer side from the consumer side, while they are much more tightly coupled
  in `CompletableFuture`.
* Number of primitives - Google futures have a small set of primitives (transform, transformAsync, catching) that
  can be combined while `CompletableFuture` has a large set of methods for combinations of the underlying primitives.
  thenApply, thenRun and thenAccept could be reduced to a single promitive and likewise for
  handle, runAfter, runAsync, whenComplete. Almost all methods in `CompletableFuture` also exist in three variants:
  regular, async with default executor and async with custom executor.   

This guide will focus on Java8+ futures since it is now the defacto standard, but most of the best practices
also apply to other types of futures.

# The details of Java 8 futures

Unfortunately, the Java 8 futures doesn't really comply with the strict definitions above.
* They are not immutable and write-once.
* Producers and consumers are not really distinct - consumers can also set the state.

That said, if you use them responsibly you can still treat them as immutable and write-once.
 
## But kind of read-only view for consumers?
Java 8+ defines the class `CompletableFuture` which implements `CompletionStage` and this class can be
seen as a both a Promise and a Future. It has all the methods necessary to fulfill a promise and to inspect
the state of the future (is it complete yet?)
 
It also defines the interface `CompletionStage` which is intended as a read-only view of future.
It contains all the methods needed to perform callbacks on a future and it does not have methods that can:
 * set the state (`complete(value)`, `obtrudeValue(value)`, etc.)
 * query the state (`get()`, `join()`, `isDone()`, etc.)

However, it does have the method `toCompletableFuture()` which means that it is in practice trivial to get back as
a future form.

You could also have other implementations of `CompletionStage` which makes it possible to hide the mutability,
but that is rarely used.

## Completing futures

A future can be completed in two ways, successful and failed.

For a successful completion, the future will contain a specific value.

For a failed completion, the future will contain a specific `Throwable` instance.
If the future fails by throwing inside a transform, or set via `completeExceptionally`,
they will be wrapped in a `CompletionException`.

If the future fails due to a cancellation, it will be mapped to a `CancellationException` instead.

> **_Experiment:_** [ExceptionTest](src/test/java/se/krka/futures/ExceptionTest.java)

# The Java 8 API
CompletableFuture has the following static methods:
* `supplyAsync(Supplier<T>)` and `supplyAsync(Supplier<T>, Executor)` - Create a future from code that runs on a different thread 
* `runAsync(Runnable, Executor)` and `runAsync(Runnable)` - Similar to supplyAsync, but returns a void future
* `completedFuture(T)` - Returns a future that's already complete
* `allOf(CompletableFuture...)` - A future that completes when all futures complete  
* `anyOf(CompletableFuture...)` - Similar to allOf, but returns as soon as any of the futures complete

Methods for introspecting the state of a future:
* `isDone()`, `isCancelled()` and `isCompletedExceptionally()` - Self-explanatory? 
* `getNumberOfDependents()` - Estimated number of callbacks for future completion (For monitoring only!) 

Methods for extracting the value of a future:
* `getNow(T default)` - Non-blocking - returns the default value if it is not complete. 
* `get()` - Blocking get - throws checked exceptions
* `get(long, TimeUnit)` - Blocking get, but with a timeout
* `join()` - Blocking get - throws unchecked exceptions 

Methods for setting the state of the future
* `complete(Object)`, `completeExceptionally(Throwable)` and `cancel(boolean)` - Complete the future in various ways 
* `obtrudeValue(Object)` and `obtrudeException(Throwable)` - Complete the future, mutating the value it even if it is already completed! 

It has a bunch of methods that operate on a future to create a new (and improved!) future:
* `thenApply(Function)` - transform value -> value
* `thenApplyAsync(Function, Executor)` - 
* `thenApplyAsync(Function)` - 
* `thenAccept(Consumer)` - 
* `thenAcceptAsync(Consumer, Executor)` - 
* `thenAcceptAsync(Consumer)` - 
* `thenRun(Runnable)` - 
* `thenRunAsync(Runnable, Executor)` - 
* `thenRunAsync(Runnable)` - 
* `thenCombine(CompletionStage, BiFunction)` - 
* `thenCombineAsync(CompletionStage, BiFunction, Executor)` - 
* `thenCombineAsync(CompletionStage, BiFunction)` - 
* `thenAcceptBoth(CompletionStage, BiConsumer)` - 
* `thenAcceptBothAsync(CompletionStage, BiConsumer, Executor)` - 
* `thenAcceptBothAsync(CompletionStage, BiConsumer)` - 
* `runAfterBoth(CompletionStage, Runnable)` - 
* `runAfterBothAsync(CompletionStage, Runnable, Executor)` - 
* `runAfterBothAsync(CompletionStage, Runnable)` - 
* `applyToEither(CompletionStage, Function)` - 
* `applyToEitherAsync(CompletionStage, Function, Executor)` - 
* `applyToEitherAsync(CompletionStage, Function)` - 
* `acceptEither(CompletionStage, Consumer)` - 
* `acceptEitherAsync(CompletionStage, Consumer, Executor)` - 
* `acceptEitherAsync(CompletionStage, Consumer)` - 
* `runAfterEither(CompletionStage, Runnable)` - 
* `runAfterEitherAsync(CompletionStage, Runnable, Executor)` - 
* `runAfterEitherAsync(CompletionStage, Runnable)` - 
* `thenCompose(Function)` - transform value -> CompletionStage(value)
* `thenComposeAsync(Function, Executor)` - 
* `thenComposeAsync(Function)` - 
* `whenComplete(BiConsumer)` - 
* `whenCompleteAsync(BiConsumer, Executor)` - 
* `whenCompleteAsync(BiConsumer)` - 
* `handle(BiFunction)` - transform either value or exception
* `handleAsync(BiFunction, Executor)` - 
* `handleAsync(BiFunction)` - 
* `exceptionally(Function)` - transform exception

There are a lot of methods here - most of them can be expressed in terms of `handle` and `thenCompose`
and can be considered convenience methods and to better express intent.

Some of them can be considered callbacks instead of transforms, but they still return futures so you can
take actions on the completion, even if you don't care about their values. 

# Running callbacks and transforms on executors

Most transform methods come in three forms. Let's use `thenApply` as an example.
It has the three methods `thenApply(function)`, `thenApplyAsync(function)` and `thenApplyAsync(function, executor)`.

`thenApply(function)` will either execute the function on the same thread that completed the parent future or execute
on the same thread that added the callback to the parent future.
Which one it is depends on if the callback was added before or after the future was completed.
Note that it is not always trivial to determine which case it is!

`thenApplyAsync(function)` is equivalent to
`thenApplyAsync(function, defaultExecutor())` which translates to `ForkJoinPool.commonPool()` in most cases.
(See javadocs for more information)
 
`thenApplyAsync(function, executor)` will schedule the function to be executed on the specified executor.

This means that for a sequence of `future.thenApplyAsync(function, executor).thenApply(otherFunction)`
you can not know for sure which thread `otherFunction` will be executed on.

> **_Experiment:_** [WhereDoesItRunTest](src/test/java/se/krka/futures/WhereDoesItRunTest.java)

## Tips and tricks
If you want to ensure that work is being done on a specific executor, you must use the `*Async` method.
If you just want to move work _away_ from the current executor, it's enough to use `*Async` method for the first
step and then use the regular methods for further calls in the chain.

Note that this only works as long as the `*Async` method is being invoked at all! If you're using `thenApplyAsync`
but the parent future has completed exceptionally, the step will be skipped, and thus the work won't move to the
specified executor.

To ensure that it always moves, I recommend using `whenCompleteAsync(() -> {}, executor)` instead. 

> **_Experiment:_** [MoveExecutorTest](src/test/java/se/krka/futures/MoveExecutorTest.java)

# Immutable? Not really

Once a future has been completed, it should not be set again, but you can in fact do it.
You can use `obtrudeValue` and `obtrudeException` to overwrite the value of a future.

If the future was already complete when it was obtruded, dependent futures will not be affected.

It's unclear in what circumstances these methods are useful. 

> **_Experiment:_** [ObtrudeTest](src/test/java/se/krka/futures/ObtrudeTest.java)

# Cancellation

Cancelling a future is the same as completing it exceptionally with a CancellationException.
The only difference is that the exact future that was cancelled will emit a CancellationException directly,
while child futures will have that exception wrapped in a CompletionException.

> **_Experiment:_** [CancelTest](src/test/java/se/krka/futures/CancelTest.java)

# Useful extra libraries

To simplify life working with Java 8 futures, there's a Spotify library called
[completable-futures](https://github.com/spotify/completable-futures).

It contains some useful utility methods, described in more detail below

## dereference

Sometimes (through no fault of you own, I'm sure!) you may end up with something like:
`CompletableFuture<CompletableFuture<T>> future` which may be annoying to work with.

Fortunately, it's not very difficult to convert that to `CompletableFuture<T> future2`.
All you need to do is apply `thenCompose(value -> value)` (composing with the identity function).

This has been wrapped as `dereference` - naming was chosen to correspond to the equivalent function in Google Guava.

## exceptionallyCompose

`exceptionallyCompose()` was introduced to cover the usecase of handling a failure by doing more asynchronous work.
While the Java 8 API lets you handle a successful future and compose it (with `thenCompose`) there is no such
 equivalent for handling exception. This convenience method is very simple to implement.

> **_Implementation:_** [ExceptionallyCompose](src/test/java/se/krka/futures/ExceptionallyCompose.java)

## combine

It also includes some utility functions to combine multiple futures in a safe way, avoiding manual error-prone calls
to get or join. 

# Testing

Writing unit tests with futures can be tricky due to the asynchronous nature of futures and the fact that
tests should run quickly and deterministically.

When possible, it's usually a good idea to pass in already completed futures to the tests and then
when verifying the result, you should use something like `CompletableFutures.getCompleted(future)`.
This ensures that the future was in fact complete and will trigger an exception if it was not.

If you instead call something like `future.get()` or `future.join()` you risk
blocking on an incomplete future - perhaps infinitely!

You also lose the benefit of being able to detect if the future is not immediately complete.
If you pass in already completed futures to the code under test, the resulting future should also be immediately
completed.

For more complex scenarios this is of course not always possible. 

# Building graphs with futures

Due to the fluent style of the future API, it's easy to think of futures as a sequence of transformation,
but we don't have to limit ourselves to that.

Multiple child futures can depend on the same parent future, and a child future can depend on multiple parent futures.
This means we should think of it as a Directed Acyclic Graph (a DAG!) instead!

Each future is a node in the graph, and each dependency is a directed edge.

# Combining futures

To create futures that depend on multiple other futures, you have multiple options:
* nest the dependencies by using `thenCompose` and `thenApply`
* use `thenCombine` to pairwise combine futures
* use `CompletableFuture.allOf()` to combine a list of futures.
* use a library such as CompletableFutures to combine up to 6 futures

> **_Experiment:_** [CombineTest](src/test/java/se/krka/futures/CombineTest.java)

## Problems with using allOf

There are some usability problems with using `allOf`, so I recommend avoiding it.

You get a `CompletableFuture<Void>` back so it is only useful to determine when the dependencies are complete,
and get a callback when it is ready. Inside the transformation or callback you then have to call join on the input futures.

This works, but has several potentials for bugs:
* If you make a mistake, you may be joining on a future that is not complete - this will block in the thread!
* If you make a mistake, you may be joining on a future that will never complete - this will deadlock the thread!
* The mistake may not be obvious, and not happen on each execution, making it hard to detect and debug. 
* If your dependencies change, you might forget to remove one of the calls to joins.  

If you still want to use `allOf()`, at least make sure to replace the calls to join (or get) with calls to
`CompletableFutures.getCompleted()` which is guaranteed to never block. It will instead fail if the future is not completed.

> **_Experiment:_** [AllOfTest](src/test/java/se/krka/futures/AllOfTest.java)

# News in Java 9

With Java 9 we got some additions to the API.

New static methods:
* `delayedExecutor(long, TimeUnit)` and `delayedExecutor(long, TimeUnit, Executor)` - Get an executor that delays the work 
* `completedStage(Object)`, `failedFuture(Throwable)` and `failedStage(Throwable)` - Complements for the `completedFuture(Object)` method 

New instance methods:
* `newIncompleteFuture()` - Virtual constructor, intended for subclassing
* `defaultExecutor()` - Get the default executor that is used for default `*Async` transforms.
* `copy()` - Convenience method for `thenApply(x -> x)`
* `minimalCompletionStage()` - Returns a more restricted future 
* `completeAsync(Supplier, Executor)` and `completeAsync(Supplier)` - Complete the future using a supplier 
* `completeOnTimeout(Object, long, TimeUnit)` - Complete the future with a value after a timeout
* `orTimeout(long, TimeUnit)` - Complete the future with an exception after a timeout

## Some notable problems
- `orTimeout` and `completeOnTimeout`
does not let you configure an executor, so you have to manually add something like:
`.whenCompleteAsync(()->{}, executor)`

Otherwise you may be bottlenecking on a static single-threaded `ScheduledExecutorService`.

> **_Experiment:_** [OnTimeoutTest](src/test/java/se/krka/futures/OnTimeoutTest.java)

# TODO:
workarounds for timeout-issue
Explain minimalCompletionStage and delayedExecutor
