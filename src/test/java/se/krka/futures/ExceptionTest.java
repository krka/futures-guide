package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ExceptionTest {

  private static final List<Class<? extends Throwable>> EXPECTED = List.of(
          CompletionException.class,
          IllegalArgumentException.class
  );

  @Test
  public void testExceptionTypeSupply() {
    Util.assertException(CompletableFuture.supplyAsync(() -> Util.doThrow(new IllegalArgumentException())), EXPECTED);
  }

  @Test
  public void testExceptionTypeSupplyWrapped() {
    Util.assertException(CompletableFuture.supplyAsync(() -> Util.doThrow(new CompletionException(new IllegalArgumentException()))), EXPECTED);
  }

  @Test
  public void testExceptionTypeComposedReturn() {
    Util.assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> CompletableFutures.exceptionallyCompletedFuture(new IllegalArgumentException())), EXPECTED);
  }

  @Test
  public void testExceptionTypeComposeWrapped() {
    Util.assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> CompletableFutures.exceptionallyCompletedFuture(new CompletionException(new IllegalArgumentException()))), EXPECTED);
  }

  @Test
  public void testExceptionTypeApplyThrow() {
    Util.assertException(CompletableFuture
            .completedFuture("value")
            .thenApply(s -> Util.doThrow(new IllegalArgumentException())), EXPECTED);
  }

  @Test
  public void testExceptionTypeComposeThrow() {
    Util.assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> Util.doThrow(new IllegalArgumentException())), EXPECTED);
  }

  @Test
  public void testExceptionTypeComposeThrowWrapped() {
    Util.assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> Util.doThrow(new CompletionException(new IllegalArgumentException()))), EXPECTED);
  }

  @Test
  public void testCancelException() {
    CompletableFuture<Object> future = new CompletableFuture<>();
    future.cancel(true);
    Util.assertException(future, List.of(CancellationException.class));
  }

}
