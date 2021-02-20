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
