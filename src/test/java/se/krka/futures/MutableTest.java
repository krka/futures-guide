package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class MutableTest {

  @Test
  public void testMutable() {
    CompletableFuture<String> future = CompletableFuture.completedFuture("value");

    CompletableFuture<String> result = compute(future);
    assertEquals("value", CompletableFutures.getCompleted(result));
  }

  @Test
  public void testMutableFail() {
    CompletableFuture<String> future = new CompletableFuture<>();

    CompletableFuture<String> result = compute(future);
    future.complete("value");

    // We get the default value instead of the expected value!
    assertEquals("default", CompletableFutures.getCompleted(result));
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