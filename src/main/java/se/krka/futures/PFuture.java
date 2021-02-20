package se.krka.futures;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

public class PFuture<T> {
  private final CompletableFuture<T> future;

  PFuture(CompletableFuture<T> future) {
    this.future = future;
  }

  public <R> PFuture<R> map(Function<T, R> function) {
    return new PFuture<>(future.thenApply(function));
  }

  public PFuture<T> tap(Consumer<T> function) {
    return new PFuture<>(future.thenApply(t -> {
      function.accept(t);
      return t;
    }));
  }

  public <R> PFuture<R> flatMap(Function<T, PFuture<R>> function) {
    return new PFuture<>(future.thenCompose(t -> function.apply(t).future));
  }

  public static <R> PFuture<R> flatten(PFuture<PFuture<R>> future) {
    return new PFuture<>(future.future.thenCompose(v -> v.future));
  }

  public boolean isDone() {
    return future.isDone();
  }

  public boolean isCompletedExceptionally() {
    return future.isCompletedExceptionally();
  }

  public boolean isCompletedNormally() {
    return future.isDone() && !future.isCompletedExceptionally();
  }


  public Throwable getException() {
    if (isCompletedExceptionally()) {
      try {
        T value = future.join();
        assert value != null;
      } catch (CancellationException e) {
        return e;
      } catch (CompletionException e) {
        return e.getCause();
      }
      throw new IllegalStateException("Unreachable code");
    } else {
      throw new IllegalStateException("Future does not have an exception");
    }
  }

  public T getValue() {
    if (isCompletedNormally()) {
      return future.join();
    } else {
      throw new IllegalStateException("Future does not have a value");
    }
  }

  @Override
  public String toString() {
    if (isCompletedExceptionally()) {
      return "PFuture(exception=" + getException().getMessage() + ")";
    } else if (isCompletedNormally()) {
      return "PFuture(value=" + getValue() + ")";
    } else {
      return "PFuture(incomplete)";
    }
  }
}
