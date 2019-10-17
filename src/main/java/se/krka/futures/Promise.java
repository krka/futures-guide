package se.krka.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

  public CompletableFuture<T> getFuture() {
    return producerFuture.thenApply(Function.identity());
  }

  public static void main(String[] args) throws InterruptedException {

    CompletableFuture<String> first = new CompletableFuture<>();
    CompletableFuture<String> abc = first
            .thenApply(x -> x + "a");

    abc.completeOnTimeout("other", 1,  TimeUnit.MILLISECONDS);

    Thread.sleep(100);
    first.complete("abc");

    System.out.println(abc.join());
    System.out.println(first.join());
  }
}
