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

  @Test
  public void testCompleteTwice() {
    CompletableFuture<String> future = new CompletableFuture<>();

    future.complete("First");
    future.complete("Second");

    assertEquals("First", getCompleted(future));
  }

  @Test
  public void testObtrude() {
    CompletableFuture<String> future = new CompletableFuture<>();

    future.complete("First");
    future.obtrudeValue("Second");

    assertEquals("Second", getCompleted(future));
  }

  @Test
  public void testObtrude2() {
    CompletableFuture<String> future = new CompletableFuture<>();

    CompletableFuture<String> child = future.thenApply(x -> x + " i am a child");

    future.complete("First");
    future.obtrudeValue("Second");

    assertEquals("Second", getCompleted(future));
    assertEquals("First i am a child", getCompleted(child));
  }
}
