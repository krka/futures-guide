package se.krka.futures;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    CompletableFuture<?> parent = new CompletableFuture<>();
    CompletableFuture<?> child = parent.thenApply(o -> o);
    parent.cancel(false);

    assertTrue(parent.isCancelled());
    assertTrue(parent.isDone());
    assertTrue(parent.isCompletedExceptionally());

    assertTrue(child.isDone());
    assertFalse(child.isCancelled()); // This was NOT explicitly cancelled!
    assertTrue(child.isCompletedExceptionally());

    Util.assertException(parent, List.of(CancellationException.class));
    Util.assertException(child, List.of(CompletionException.class, CancellationException.class));
  }

  @Test
  public void cancelChild() throws InterruptedException, ExecutionException, TimeoutException {
    CompletableFuture<?> parent = new CompletableFuture<>();
    CompletableFuture<?> child = parent.thenApply(o -> o);
    child.completeExceptionally(new IllegalArgumentException());

    assertFalse(parent.isCancelled());
    assertFalse(parent.isDone());
    assertFalse(parent.isCompletedExceptionally());

    assertTrue(child.isDone());
    assertFalse(child.isCancelled());
    assertTrue(child.isCompletedExceptionally());

    Util.assertException(child, List.of(CompletionException.class, IllegalArgumentException.class));

  }
}
