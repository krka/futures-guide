package se.krka.futures;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.spotify.futures.CompletableFutures.getCompleted;
import static org.junit.Assert.assertEquals;

public class CompleteTest {
  @Test
  public void testComplete() {
    CompletableFuture<String> future = new CompletableFuture<>();
    CompletableFuture<String> future2 = future.thenApply(Function.identity());

    // This is usually not a very useful thing to do, since future2 is defined to depend on future
    future2.complete("Second");

    future.complete("First");

    assertEquals("First", getCompleted(future));
    assertEquals("Second", getCompleted(future2));
  }
}
