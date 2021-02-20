package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class ExceptionTest {

  private static final List<Class<? extends Throwable>> EXPECTED = List.of(
          CompletionException.class,
          IllegalArgumentException.class
  );

  @Test
  public void testExceptionSimple() {
    CompletableFuture<Object> future = new CompletableFuture<>();
    future.completeExceptionally(new IllegalArgumentException());
    assertEquals(IllegalArgumentException.class, Util.exceptionFromCallback(future).getClass());
    Util.assertException(future, List.of(CompletionException.class, IllegalArgumentException.class));

    // Wrapped, since it has gotten a transform operation!
    CompletableFuture<Object> future2 = future.thenApply(x -> x);
    assertEquals(CompletionException.class, Util.exceptionFromCallback(future2).getClass());
    Util.assertException(future2, List.of(CompletionException.class, IllegalArgumentException.class));

    // Same for rethrowing the exception in a transform
    CompletableFuture<Object> future3 = future.exceptionally(e -> Util.doThrow((RuntimeException) e));
    assertEquals(CompletionException.class, Util.exceptionFromCallback(future3).getClass());
    Util.assertException(future3, List.of(CompletionException.class, IllegalArgumentException.class));

    // Same for doing a copy of the future
    CompletableFuture<Object> future4 = future.copy();
    assertEquals(CompletionException.class, Util.exceptionFromCallback(future4).getClass());
    Util.assertException(future4, List.of(CompletionException.class, IllegalArgumentException.class));

  }

  @Test
  public void testExceptionTypeSupply() {
    CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> Util.doThrow(new IllegalArgumentException()));
    Util.assertException(future, EXPECTED);
    assertEquals(CompletionException.class, Util.exceptionFromCallback(future).getClass());
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
    assertEquals(CancellationException.class, Util.exceptionFromCallback(future).getClass());
    Util.assertException(future, List.of(CancellationException.class));
  }

  @Test
  public void testCancelException2() {
    CompletableFuture<Object> future = new CompletableFuture<>();
    future.completeExceptionally(new CancellationException());
    assertEquals(CancellationException.class, Util.exceptionFromCallback(future).getClass());
    Util.assertException(future, List.of(CancellationException.class));
  }

  @Test
  public void testTimeoutException() {
    CompletableFuture<Object> future = new CompletableFuture<>();
    future.completeExceptionally(new TimeoutException());
    assertEquals(TimeoutException.class, Util.exceptionFromCallback(future).getClass());
    Util.assertException(future, List.of(CompletionException.class, TimeoutException.class));
  }

}
