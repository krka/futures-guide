package se.krka.futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Alternative wrapper-implementation that separates futures from promises
 */
public final class Promise<T> {

  private final CompletableFuture<T> producerFuture = new CompletableFuture<>();

  private Promise() {
  }

  public static <T> Promise<T> newPromise() {
    return new Promise<>();
  }

  public void complete(T value) {
    producerFuture.complete(value);
  }

  public void completeExceptionally(Throwable ex) {
    producerFuture.completeExceptionally(ex);
  }

  public void cancel() {
    producerFuture.cancel(false);
  }

  public PFuture<T> getFuture() {
    return new PFuture<>(producerFuture);
  }
}
