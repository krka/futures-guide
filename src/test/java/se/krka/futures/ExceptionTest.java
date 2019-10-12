package se.krka.futures;

import com.google.common.collect.ImmutableList;
import com.spotify.futures.CompletableFuturesExtra;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.assertEquals;

public class ExceptionTest {

  private static final ImmutableList<Class<?>> EXPECTED = ImmutableList.of(
          CompletionException.class,
          IllegalArgumentException.class
  );

  @Test
  public void testExceptionTypeSupply() {
    assertException(CompletableFuture.supplyAsync(() -> Util.doThrow(new IllegalArgumentException())));
  }

  @Test
  public void testExceptionTypeSupplyWrapped() {
    assertException(CompletableFuture.supplyAsync(() -> Util.doThrow(new CompletionException(new IllegalArgumentException()))));
  }

  @Test
  public void testExceptionTypeComposedReturn() {
    assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> CompletableFuturesExtra.exceptionallyCompletedFuture(new IllegalArgumentException())));
  }

  @Test
  public void testExceptionTypeComposeWrapped() {
    assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> CompletableFuturesExtra.exceptionallyCompletedFuture(new CompletionException(new IllegalArgumentException()))));
  }

  @Test
  public void testExceptionTypeApplyThrow() {
    assertException(CompletableFuture
            .completedFuture("value")
            .thenApply(s -> Util.doThrow(new IllegalArgumentException())));
  }

  @Test
  public void testExceptionTypeComposeThrow() {
    assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> Util.doThrow(new IllegalArgumentException())));
  }

  @Test
  public void testExceptionTypeComposeThrowWrapped() {
    assertException(CompletableFuture.completedFuture("value")
            .thenCompose(s -> Util.doThrow(new CompletionException(new IllegalArgumentException()))));
  }

  private static void assertException(CompletableFuture<Object> input) {
    CompletableFuture<?> future = input
            .exceptionally(throwable -> ImmutableList.of(throwable.getClass(), throwable.getCause().getClass()));
    assertEquals(EXPECTED, future.join());
  }

}
