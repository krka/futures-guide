package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class ObtrudeTest {
  @Test
  public void testObtrudeAfterCallback() {
    CompletableFuture<String> future = CompletableFuture.completedFuture("first");
    CompletableFuture<String> future2 = future.thenApply(v -> v + " second");

    assertEquals("first", CompletableFutures.getCompleted(future));
    assertEquals("first second", CompletableFutures.getCompleted(future2));

    future.obtrudeValue("not-first");
    assertEquals("not-first", CompletableFutures.getCompleted(future));
    assertEquals("first second", CompletableFutures.getCompleted(future2));
  }

  @Test
  public void testObtrudeBeforeCallback() {
    CompletableFuture<String> future = new CompletableFuture<>();
    CompletableFuture<String> future2 = future.thenApply(v -> v + " second");

    future.obtrudeValue("first");
    assertEquals("first", CompletableFutures.getCompleted(future));
    assertEquals("first second", CompletableFutures.getCompleted(future2));

    future.obtrudeValue("not-first");
    CompletableFuture<String> future3 = future.thenApply(v -> v + " second");

    assertEquals("not-first second", CompletableFutures.getCompleted(future3));
  }
}
