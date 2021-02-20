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

# Changes between Java 10 and Java 15

Nothing has changed - this API seems to have stabilized.
