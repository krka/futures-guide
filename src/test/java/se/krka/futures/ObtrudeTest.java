package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class ObtrudeTest {
  @Test
  public void testObtrudeAfterCallback() {
    CompletableFuture<String> parent = CompletableFuture.completedFuture("first");
    CompletableFuture<String> child = parent.thenApply(v -> v + " second");

    assertEquals("first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));

    parent.obtrudeValue("not-first");
    assertEquals("not-first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));
  }

  @Test
  public void testObtrudeBeforeCallback() {
    CompletableFuture<String> parent = new CompletableFuture<>();
    CompletableFuture<String> child = parent.thenApply(v -> v + " second");

    parent.obtrudeValue("first");
    assertEquals("first", CompletableFutures.getCompleted(parent));
    assertEquals("first second", CompletableFutures.getCompleted(child));

    parent.obtrudeValue("not-first");
    CompletableFuture<String> secondChild = parent.thenApply(v -> v + " second");

    assertEquals("not-first second", CompletableFutures.getCompleted(secondChild));
  }
}
