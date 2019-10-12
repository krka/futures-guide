# Futures lessons

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

(There is a good [wikipedia article](https://en.wikipedia.org/wiki/Futures_and_promises) about this
if you want to read more about it)

# The Java caveats

Unfortunately, the real world in Java doesn't look like what's described above.

## Not really write-once
Java future can be written more than once

## Not really read-only for consumers
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

## Futures are similar to monads

## States

Futures are in one of two states: incomplete and complete
They can transition from incomplete to complete but not the other way around.

We typically operate on futures as if they were immutable but they can be abused to not be.

## Cached computation

## Obtruding values

# Primitives

## Async methods
## Extra functionality

# CompletableFuture vs CompletionStage

# Exceptions

# Threads and executors

# Testing

## Avoid blocking

# Combining futures

## Avoid allOf + get

# Stuff missing from Java 8 API
## failedFuture

# News in Java 9

## Still missing
### onTimeout and completeOnTimeout
does not let you configure an executor, so you have to manually add something like:
`.whenCompleteAsync(()->{}, executor)`

Otherwise you may be bottlenecking on a static single-threaded ScheduledExecutorService.