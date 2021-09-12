# Writing concurrent code and avoiding blocking gets

Given a future, you can call `future.get()` or `future.join()` to get its value.
If the future is completed, either normally or exceptionally, this is a non-blocking method call that returns immediately.
However, if the future is not completed, this will block the current thread until the the future has completed.

## The problem
Calling `get` or `join` in this way is not always a problem. There are several types of code where this makes sense to do:

* You have a background thread or scheduled executor that performs some update periodically.
  The only thread you block is that specific background thread, and it's only used for that purpose anyway.
* You are fetching some asynchronous data as part of system startup. Since this just happens rarely, blocking the thread is fine, and perhaps unavoidable.
* You are in a critical path of service code, but you *know* that this future is complete so calling `get` or `join` will not be blocking.

However, in other cases, this problem that happens can be very subtle and hard to diagnose:

* All threads in the threadpool gets blocked which means that the service slows down to a crawl even though CPU utilization is low.
* Threads may end up causing deadlocks, since other threads may wait for this thread to complete its work.
* Dynamic threadpools may start growing indefinitely when the threadpool detects that there are no available threads.
  This will start consuming a lot of memory, possibly cause memory leaks and increase the overhead related to threading.

The symptoms may be hard to spot. It may be the case that due to the ordering of operations and behaviour of other asynchronous systems, the call to `get` or `join`
may usually not be blocking, so the problematic symptoms are not noticed when the problematic code is added,
but is instead only noticed much later, when some other disturbance in the normal flow of operations triggers it.

## Solutions

There are many strategies that can be used to circumvent these problems.

### Avoidance

If you're already working with asynchronous APIs - i.e. you are either expected to return a `CompletionStage` or `CompletableFuture`
or you are expected to invoke som callback such as `StreamObserver.onNext(...)` there is generally no need to call `get` or `join` at all.

Each such call could be replaced with some combinations of transformations on the future.
Exactly how to do this varies by case but it usually involves the following:

* Methods that previously called `get` or `join` and returned value will need to return a future instead.
* Methods that take futures as inputs should take raw values as input instead, and the method should be called as part of a future transformation instead.

Sometimes the work needed to transform the code can be complicated, but it should always be possible.

### Assert that the future is completed.

If you really don't want to go the avoidance path, you can make other improvements that require a smaller investment in refactoring.

When you write code that calls `get` or `join`, you may reason that the future should always be completed once it reaches that part in the code.
This may very well be correct, but the code may be changed over time, and other developers (or yourself), may not be aware of that invariant.

Instead, it's better to assert that invariant.

If you depend on the [completable-futures](https://github.com/spotify/completable-futures) library you can use the method
[CompletableFutures.getCompleted](https://github.com/spotify/completable-futures/blob/master/src/main/java/com/spotify/futures/CompletableFutures.java#L275)
instead of `get` or `join`.

The implementation is simple:

```java
    if (!future.isDone()) {
      throw new IllegalStateException("future was not completed");
    }
    return future.join();
```

This means that if your invariant changes in the future, your code will not just silently start to exhibit blocking behavior and random latency spikes but instead
you will see exceptions that directly correspond to a violation of this invariant.

Note that this will still not catch the problem until it occurs. It may very well be the that this never triggers during normal operations, but suddenly it starts
breaking when something rare triggers it.

### Transforming and combining

If you depend on a single future, refactoring to use `.thenApply` or `.thenCompose` instead of `get` or `join` may be straightforward.

If you depend on two futures you can use `.thenCombine`.

For more futures than that, you have to get more creative.
For three or more futures you can use this pattern: `f1.thenCompose(v1 -> f2.thenCompose(v2 -> f3.thenApply(v3 -> func(v1, v2, v3))))`.

However that can become hard to read if it gets heavily nested.

An alternative is to use the `combine` methods from [completable-futures](https://github.com/spotify/completable-futures):

The code can than be written as `CompletableFutures.combine(f1, f2, f3, (v1, v2, v3) -> func(v1, v2, v3))` which is a bit cleaner.
We have to make sure to pass in the futures in the same order as the lambda parameters.

If the number of futures grows a lot, you can instead use the more generic combiner:

```java
Future<T> f1;
Future<T> f2;
// ...
Future<T> f20;
CompletableFutures.combine(combined -> func(combined), f1, f2, ..., f20);
```

Here we get the transform function first and then we pass in the futures (in any order).
The combined object can then be used like this: `v1 = combined.get(f1)`.

This looks similar to a blocking `get` or `join` but this operation will always complete immediately, and it will throw an exception if
the future you pass into it is not part of the call to combine.

That means that this approach is more safe than using `getCompleted` since it will always detect cases where the code is failing to combine the right futures
unlike `getCompleted` which can not detect the problems until it has already gone wrong.
