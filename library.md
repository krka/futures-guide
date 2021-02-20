# Completable-futures library

To simplify life working with Java 8+ futures, there's a Spotify library called
[completable-futures](https://github.com/spotify/completable-futures).

It contains some useful utility methods, described in more detail below

## dereference

Sometimes (through no fault of you own, I'm sure!) you may end up with something like:
`CompletableFuture<CompletableFuture<T>> future` which may be annoying to work with.

Fortunately, it's not very difficult to convert that to `CompletableFuture<T> future2`.
All you need to do is apply `thenCompose(value -> value)` (composing with the identity function).

This has been wrapped as `dereference` - naming was chosen to correspond to the equivalent function in Google Guava.

## getCompleted and getException

These functions can simplify getting the value or exception from a completed future, without risking blocking the thread.

This is typically useful in code that needs the value from known completed futures or in unit tests.

## exceptionallyCompose

`exceptionallyCompose()` was introduced to cover the usecase of handling a failure by doing more asynchronous work.
While the Java 8 API lets you handle a successful future and compose it (with `thenCompose`) there is no such
 equivalent for handling exception. This convenience method is very simple to implement.

> **_Implementation:_** [ExceptionallyCompose](src/test/java/se/krka/futures/ExceptionallyCompose.java)

## combine

It also includes some utility functions to combine multiple futures in a safe way, avoiding manual error-prone calls
to get or join. 

## And more...

Check out the github page to learn about more utilities.