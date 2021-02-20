package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AllOfTest {
  @Test
  public void testAllOfCorrect() {
    CompletableFuture<String> futureA = new CompletableFuture<>();
    CompletableFuture<String> futureB = new CompletableFuture<>();
    CompletableFuture<String> futureC = new CompletableFuture<>();

    CompletableFuture<Void> futureAll = CompletableFuture.allOf(futureA, futureB, futureC);
    CompletableFuture<String> joined = futureAll.thenApply(aVoid -> futureA.join() + futureB.join() + futureC.join());

    futureA.complete("A");
    futureB.complete("B");
    futureC.complete("C");

    assertEquals("ABC", joined.join());
  }

  @Test
  public void testAllOfIncorrect() {
    expectTimeout(() -> {
      CompletableFuture<String> futureA = new CompletableFuture<>();
      CompletableFuture<String> futureB = new CompletableFuture<>();
      CompletableFuture<String> futureC = new CompletableFuture<>();
      CompletableFuture<String> futureD = new CompletableFuture<>();

      // Oops, forgot to include futureD here!
      CompletableFuture<Void> futureAll = CompletableFuture.allOf(futureA, futureB, futureC);
      CompletableFuture<String> joined = futureAll.thenApply(aVoid -> futureA.join() + futureB.join() + futureC.join() + futureD.join());

      futureA.complete("A");
      futureB.complete("B");
      futureC.complete("C");

      // futureAll is complete now, but callback is deadlocked!

      joined.join();
    });
  }

  private void expectTimeout(Runnable runnable) {
    try {
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        runnable.run();
        return null;
      });
      future.get(1, TimeUnit.SECONDS);
      fail("Unexpected success");
    } catch (InterruptedException | ExecutionException e) {
      fail("Unexpected exception: " + e.getMessage());
    } catch (TimeoutException e) {
      // Expected
    }
  }

  @Test
  public void testBetterApproach() {
    CompletableFuture<String> futureA = new CompletableFuture<>();
    CompletableFuture<String> futureB = new CompletableFuture<>();
    CompletableFuture<String> futureC = new CompletableFuture<>();

    CompletionStage<String> futureAll = CompletableFutures.combine(
            futureA, futureB, futureC,
            (a, b, c) -> a + b + c);

    futureA.complete("A");
    futureB.complete("B");
    futureC.complete("C");

    assertEquals("ABC", futureAll.toCompletableFuture().join());
  }
}
