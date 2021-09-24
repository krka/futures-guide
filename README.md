# The opinionated guide to Java 8+ Futures

This is intended as quick guide to getting started with futures in Java.
It includes some theory, some best practices, exercise material and known issues to keep in mind.
Hopefully all of this can be useful to get a better understanding of how futures work in Java 8+.

We will refer to instances of `CompletableFuture<T>` as simply future.

# Part 1 - Introduction and basic concepts

Let us start with defining what a future is.

## Futures as value containers

A future is a container for a value, similar to an `AtomicReference<T>`.
It has two fundamental states - incomplete (a value has not been set yet) and complete (a specific value has been set).
More specifically, it can be completed in two different ways - normally with a value, and exceptionally with an exception. 

The following chart shows the possible state transitions. 
```
                                                          Completed with value
  Incomplete future               complete              +----------------------+
+----------------------------+  +---------------------> |                      |
|                            |    obtrude               | A Value              |
|  Listeners                 |                          |                      |
| +---------------------+    |                          +----------------------+
| |+                     +   |                                |  |
| | +                     +  |                                |  | obtrude and
| |  +---------------------+ |                                |  | obtrudeException
| +  |                     | |                                v  v
|  + |                     | |                            Completed with exception
|   +|                     | |                          +--------------------------+
|    +---------------------+ |   completeExceptionally  |                          |
|                            |  +---------------------> | An exception             |
+----------------------------+   obtrudeException       |                          |
                                                        +--------------------------+
```
A future can go from incomplete to complete, but never back again.

Listeners can be seen as a list of tuples of `(Runnable, Executor)`.
When the future changes state from incomplete to complete, the runnables are invoked on their respective executors, and
the list of listeners is cleared. At this point, the future will never again have any listeners. 

A completed future can still be mutated, using the `obtrude*`-methods, but that's generally not a very common
practice. If the future was already complete when it was obtruded, dependent futures will not be affected.

> **_Experiment:_** [ObtrudeTest](src/test/java/se/krka/futures/ObtrudeTest.java)

## Thread safety

A nice thing about futures is that they are thread safe.
It's completely safe to do concurrent completions of a future (even though that is not a common pattern).
Only one of the completions will take effect, and listeners will only be triggered once.

It is also safe to add listeners concurrently, and to add a listener at the same time as the future is completed.
Listeners are guaranteed to either be invoked exactly once if the future is completed
(and never invoked if the future is never completed).

## Transformations on futures

It is very common to apply transformations on futures. This is similar to how we can apply
transformations on `Optional<T>` objects. Applying transformations does not modify the original future
but instead creates a new future. The transformations are _eager_ which means that transformations will be
applied as soon as the future completes (or immediately if the future is already complete).

These transformations are of course implemented by using the internal listeners in the future object.
Applying a transformation is functionally the same thing as creating an incomplete future and then
attaching a listener to the original future that will complete the new feature.

You can imagine it being implemented something similar to this:

```java
  CompletableFuture<A> inputFuture;
  CompletableFuture<B> outputFuture = inputFuture.thenApply(function);
```

The implementation is then (massively simplified):
```java
  CompletableFuture<B> thenApply(Function<A, B> function) {
    CompletableFuture<B> result = new CompletableFuture<>();
    if (isDone()) {
      completeWith(result, function);
    } else {
      this.addListener(() -> completeWith(result, function));
    }
    return result;
  }

  private void completeWith(CompletableFuture<B> result, Function<A, B> function) {
    assert this.isDone();
    try {
      A inputValue = this.get();
      B outputValue = function.apply(inputValue);
      result.complete(outputValue);
    } catch (Exception e) {
      result.completeExceptionally(e);
    }
  }
```

Note that this means that the listeners are only needed for incomplete futures,
since the transformation is computed immediately for completed futures.

This means that a transformation that is set on a completed future will execute the supplied function on the thread
that *created* the transformation, and a transformation that runs on an incomplete future will execute the
supplied function on the thread that *completed* the future.

This can be very important to keep in mind if the functions are expensive to use, or perhaps even blocking, since
it could lead to starving important thread pools by mistake. 

## Async futures and executors

Most of the transformation functions also lets you do things asynchronously, either by passing in an executor
explicitly (i.e. `thenApplyAsync(function, executor)`) or by implicitly using the default ForkJoinPool
(i.e. `thenApplyAsync(function)`).

Note that using the Async variants is the only way to ensure that work is being run on a specific executor,

## Futures and stages

In addition to the concrete class `CompletableFuture<T>` there is an interface called `CompletionStage<T>`.
You can downcast a `CompletableFuture<T>` to `CompletionStage<T>` and you can
call `CompletionStage.toCompletableFuture()` to get a future back.

This means that in practice, these are effectively the same thing, and the major difference is a view of which
operations are available.

`CompletionStage` is intended as a read-only view of future.
It contains all the methods needed to perform callbacks on a future and it does not have methods that can:
 * set the state (`complete(value)`, `obtrudeValue(value)`, etc.)
 * query the state (`get()`, `join()`, `isDone()`, etc.)

## Building graphs with futures

Due to the fluent style of the future API, it's easy to think of futures as a sequence of transformation,
but we don't have to limit ourselves to that.

Multiple child futures can depend on the same parent future, and a child future can depend on multiple parent futures.
This means we should think of it as a Directed Acyclic Graph (a DAG!) instead!

Each future is a node in the graph, and each dependency is a directed edge.

# Part 2 - Why do we need futures?

The previous section discussed what futures are and how they work, it is important to understand why we need
futures at all.

## Easier and more well defined asynchronous programming

Futures give us a nice tool to effectively create a computation graph of deferred work without having to manually
manage any other concurrency primitives such as locks, synchronization, semaphores, etc. This means that you can
more easily avoid deadlocks, blocking threads and other types of concurrency bugs.

## Easier parallelism

With a synchronous code flow, it's not easy to run multiple things in parallel and then wait for the results.
You would need to submit work to executors and then block until both tasks are complete. This can get messy with more
complex setups.

Using futures, we can operate on the futures monadically almost as if we're operating on the values directly.
While this adds some verbosity, we can at least maintain a (mostly) equivalent flow of logic.

## Better utilization of hardware

Threads are (currently at least) a fairly expensive primitive, both in terms of memory usage and context switching
overhead.
If we can change the code from using a large number of threads that each blocks to wait on results,
to a large number of futures that never block, but instead run on a shared set of thread pools and voluntarily hand off
work, we can improve the hardware utilization.

# Part 3 - Important details about Java futures

Unfortunately, some things are not very intuitive, so in addition to understanding the basic concepts it is
also important to know some of the details of how the futures operate.
 
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

## Exception handling

A future can be completed in two ways, with a value and with an exception.

For exceptionally completed futures, the actual exceptions you can observe are not always the same type.

* If you complete a future with a specific exception and then observe it via `exceptionally()`, `handle()` or `whenComplete()`
  you will see the same exception.
* If you observe a future that is transformation of another exceptionally completed future, the exception will be
  wrapped in a `CompletionException` (unless it already is).
* If you catch an exception by calling `get()`, a potential `CompletionException` will be unwrapped and wrapped
  in an `ExecutionException` 
* If you catch an exception by calling `join()`, the exception will be wrapped in a `CompletionException`
  (unless it already is).

These rules are not perfectly accurate - there are some other edge cases, especially regarding CancellationException.

Since it is not always known in your code if you are operating on a direct future that can be completed or a transform,
it's recommended to always unwrap any potential `CompletionException`
The rules for how exceptions are handled are unfortunately somewhat messy, so some generic advice would be:
* When you get an exception, use a convenience method to unwrap any `CompletionException` before operating on it.

> **_Experiment:_** [ExceptionTest](src/test/java/se/krka/futures/ExceptionTest.java)

## Cancellation

Cancelling a future is the same as completing it exceptionally with a CancellationException.
The only difference is that the exact future that was cancelled will emit a CancellationException directly,
while child futures will have that exception wrapped in a CompletionException.

> **_Experiment:_** [CancelTest](src/test/java/se/krka/futures/CancelTest.java)

## Timeouts

Timeouts are very similar to cancellations. The `orTimeout()` method can cause the future to complete exceptionally
with a `TimeoutException`. Note that `orTimeout()` is not a transformation of a future, it just schedules a job
to complete it exceptionally after a specific time, if it has not been otherwise completed by that time.
This is also why it is not part of the `CompletionStage` interface.

`completeOnTimeout()` is very similar, except it allows you to complete it with a specific value instead of an exception.

# Part 4 - Common patterns and best practices

This section describes some common problems and their solutions. Note that some of the patterns are sometimes
overly verbose and can be replaced with helper functions. Fortunately, you may not have to write those functions
yourself and one such library is described here: [completable-futures](library.md)

## Composition of futures

A common operation is to create a sequence of asynchronous calls.
As an example, consider a flow where you make a remote http call and then use the result of that call to make a second
call.

Expressed as a future, you could implement this as:

```java
  remoteCall1().thenCompose(result -> remoteCall2(result)
``` 

This is similar to `thenApply` except that the function is expected to return a future instead of a value.

What if you want to recover from an exception by making a remote call?
```java
  remoteCall1().exceptionally(exception -> remoteCall2())
``` 
Unfortunately, this won't work, since `exceptionally()` requires a function that returns the same type as its
parent future. And even if it would work, you would now have a `CompletableFuture<CompletableFuture<T>>` instead
of a `CompletableFuture<T>`.

There is a common pattern you can use to solve it:
```java
  remoteCall1()
    .thenApply(value -> CompletableFuture.completed(value)) // Wrap in extra layer of futures
    .exceptionally(exception -> remoteCall2())
    .thenCompose(future -> future) // Unwrap the extra layer
``` 

By first transforming the future into a future of futures, we can make the exceptional step work.
After that we can transform it back to the original type using compose with the identity function.

If you use [completable-futures](library.md) there is a function called `exceptionallyCompose` you can use instead.

## Combining futures

Another common scenario is that you have multiple futures and require all of those values to make progress.

There are multiple ways of achieving that:

```java
  future1.thenCompose(value1 -> future2.thenApply(value2 -> func(value1, value2)))
``` 
Thanks to the scoping rules of lambdas, you can access the first future's value from the inner lambda.
This can be generalized for more futures, but the code may end up looking very complex.

A cleaner alternative for pairs of futures is using the built in method:
```java
  future1.thenCombine(future2, (value1, value2) -> func(value1, value2))
``` 
But it only works for pairs.

For more complex cases, you can use `CompletableFuture.allOf(futures)` to combine multiple futures and
then use on `join()` to access the actual values.
This is however somewhat error prone and not recommended.


To summarize, there are multiple alternatives:
* nest the dependencies by using `thenCompose` and `thenApply`
* use `thenCombine` to pairwise combine futures
* use `CompletableFuture.allOf()` to combine a list of futures.
* use a library such as [completable-futures](library.md) to combine more futures.

> **_Experiment:_** [CombineTest](src/test/java/se/krka/futures/CombineTest.java)


If you use [completable-futures](library.md) there is a function called `combine` for more advanced use cases.

## Joining on completed futures

If you do need to join on completed futures, it is best to wrap that call in `isDone()`. This helps avoid
the risk of accidentally blocking the thread if a bug is introduced:
```java
  if (future.isDone()) {
    return future.join();
  }
```

If you use [completable-futures](library.md) there is a function called `getCompleted` you can use instead.

## Testing

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

## Moving work from executors

If you want to ensure that work is being done on a specific executor, you must use the `*Async` method.
If you just want to move work _away_ from the current executor, it's enough to use `*Async` method for the first
step and then use the regular methods for further calls in the chain.

Note that this only works as long as the `*Async` method is being invoked at all! If you're using `thenApplyAsync`
but the parent future has completed exceptionally, the step will be skipped, and thus the work won't move to the
specified executor.

To ensure that it always moves, I recommend using `future.whenCompleteAsync((v, e) -> {}, executor)` instead. 

> **_Experiment:_** [MoveExecutorTest](src/test/java/se/krka/futures/MoveExecutorTest.java)

## Avoiding joins.

If you are writing some method that returns a CompletionStage or a CompletableFuture, there's never any need to
call `get()` or `join()` on a future.

Instead prefer to transform and combine futures to get into the desired state.

In some cases you may realize that some futures are guaranteed to be complete, based on the flow of the code,
but even then calling `get()` or `join()` should be avoided.
While it is fully possible to do this in a correct way, there is a big risk of introducing bugs in the future by
writing code in that way.

# Part 5 - Common pitfalls

## Executor for timeouts

`orTimeout` and `completeOnTimeout` does not let you configure an executor, so you should manually move the work
to a different executor. Otherwise you may be bottlenecking on a static single-threaded `ScheduledExecutorService`.

### Problematic example

```java
return future.orTimeout(...);
```
if the future times out, all other futures that apply computations based on the result of that future will run on that
static single-thread executor.

### Workaround

Instead, use this pattern:
```java
return future.orTimeout(...).whenCompleteAsync((value, throwable) -> {}, executor);
```

to ensure that regardless of whether the future completes before the timeout or after the timeout, the remaining work will
be done on your specified executor.

> **_Experiment:_** [OnTimeoutTest](src/test/java/se/krka/futures/OnTimeoutTest.java)

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

## Futures and mutable state

If you look at futures from the consumer side and focus on the transformation operations, you can see them
as immutable. The futures themselves don't change, they just contain a value. From the consumer side,
it doesn't matter if the futures are complete or not, they will eventually apply the transformations.

This also means that you shouldn't mix futures with mutable objects -
that introduces risks of race condition related bugs!  

> **_Experiment:_** [MutableTest](src/test/java/se/krka/futures/MutableTest.java)
