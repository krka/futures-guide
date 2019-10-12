package se.krka.futures;

import com.spotify.futures.CompletableFuturesExtra;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class MutableTest {

  @Test
  public void testMutable() {
    CompletableFuture<String> future = CompletableFuture.completedFuture("value");

    CompletableFuture<String> result = compute(future);
    assertEquals("value", CompletableFuturesExtra.getCompleted(result));
  }

  @Test
  public void testMutableFail() {
    CompletableFuture<String> future = new CompletableFuture<>();

    CompletableFuture<String> result = compute(future);
    future.complete("value");

    assertEquals("value", CompletableFuturesExtra.getCompleted(result));
  }

  private CompletableFuture<String> compute(CompletableFuture<String> future) {
    AtomicReference<String> mutable = new AtomicReference<>("default");
    future.thenApply(v -> {
      mutable.set(v);
      return null;
    });
    return CompletableFuture.completedFuture(mutable.get());
  }
}