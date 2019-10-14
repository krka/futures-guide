package se.krka.futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ExceptionallyCompose {
  public static <T> CompletableFuture<T> exceptionallyCompose(
          CompletableFuture<T> input,
          Function<Throwable, CompletableFuture<T>> fun) {
    CompletableFuture<CompletableFuture<T>> wrapped = input.thenApply(value -> CompletableFuture.completedFuture(value));
    CompletableFuture<CompletableFuture<T>> wrappedWithException = wrapped.exceptionally(throwable -> fun.apply(throwable));
    CompletableFuture<T> unwrapped = wrappedWithException.thenCompose(future -> future);
    return unwrapped;
  }
}
