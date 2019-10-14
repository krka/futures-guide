package se.krka.futures;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CancelTest {
  @Test
  public void cancelSimple() {
    CompletableFuture<?> future = new CompletableFuture<>();
    future.cancel(true);
    Util.assertException(future, List.of(CancellationException.class));
  }

  @Test
  public void cancelWithException() {
    CompletableFuture<?> future = new CompletableFuture<>();
    future.completeExceptionally(new CancellationException());
    Util.assertException(future, List.of(CancellationException.class));
  }

  @Test
  public void cancelParent() {
    CompletableFuture<?> future = new CompletableFuture<>();
    CompletableFuture<?> future2 = future.thenApply(o -> o);
    future.cancel(false);

    assertTrue(future.isCancelled());
    assertTrue(future.isDone());
    assertTrue(future.isCompletedExceptionally());

    assertTrue(future2.isDone());
    assertFalse(future2.isCancelled()); // This was NOT explicitly cancelled!
    assertTrue(future2.isCompletedExceptionally());

    Util.assertException(future, List.of(CancellationException.class));
    Util.assertException(future2, List.of(CompletionException.class, CancellationException.class));
  }

  @Test
  public void cancelChild() {
    CompletableFuture<?> future = new CompletableFuture<>();
    CompletableFuture<?> future2 = future.thenApply(o -> o);
    future2.cancel(false);

    assertFalse(future.isCancelled());
    assertFalse(future.isDone());
    assertFalse(future.isCompletedExceptionally());

    assertTrue(future2.isDone());
    assertTrue(future2.isCancelled());
    assertTrue(future2.isCompletedExceptionally());

    Util.assertException(future2, List.of(CancellationException.class));

  }
}
