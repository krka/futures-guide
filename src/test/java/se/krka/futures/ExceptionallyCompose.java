package se.krka.futures;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ExceptionallyCompose {
  public static <T> CompletableFuture<T> exceptionallyCompose(
          CompletableFuture<T> input,
          Function<Throwable, CompletableFuture<T>> fun) {
    return input.thenApply(CompletableFuture::completedFuture).exceptionally(fun::apply).thenCompose(future -> future);
  }

  public static void main(String[] args) {

    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(new RuntimeException());

    //CompletableFuture<String> foo = future.exceptionally(throwable -> callServiceAgain());
  }

  private static CompletableFuture<String> callServiceAgain() {
    return CompletableFuture.completedFuture("It worked now");
  }
}
