package se.krka.futures;

import com.spotify.futures.CompletableFutures;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.assertEquals;

public class CombineTest {
  @Test
  public void testCombineWithNesting() {
    CompletableFuture<String> futureA = CompletableFuture.completedFuture("A");
    CompletableFuture<String> futureB = CompletableFuture.completedFuture("B");
    CompletableFuture<String> futureC = CompletableFuture.completedFuture("C");

    CompletableFuture<String> result = futureA.thenCompose(valueA -> futureB.thenCompose(valueB -> futureC.thenApply(valueC -> valueA + valueB + valueC)));
    assertEquals("ABC", CompletableFutures.getCompleted(result));
  }

  @Test
  public void testCombineWithThenCombine() {
    CompletableFuture<String> futureA = CompletableFuture.completedFuture("A");
    CompletableFuture<String> futureB = CompletableFuture.completedFuture("B");
    CompletableFuture<String> futureC = CompletableFuture.completedFuture("C");

    CompletableFuture<String> result = futureA.thenCombine(futureB, (valueA, valueB) -> valueA + valueB).thenCombine(futureC, (valueAB, valueC) -> valueAB + valueC);
    assertEquals("ABC", CompletableFutures.getCompleted(result));
  }

  @Test
  public void testCombineWithLibrary() {
    CompletableFuture<String> futureA = CompletableFuture.completedFuture("A");
    CompletableFuture<String> futureB = CompletableFuture.completedFuture("B");
    CompletableFuture<String> futureC = CompletableFuture.completedFuture("C");

    CompletionStage<String> result = CompletableFutures.combine(futureA, futureB, futureC,
            (valueA, valueB, valueC) -> valueA + valueB + valueC);
    assertEquals("ABC", CompletableFutures.getCompleted(result));
  }

  public static void combine(CompletableFuture<String> future1, CompletableFuture<String> f2, CompletableFuture<String> f3) {
    CompletableFuture<String> result = future1.thenCompose(
            value -> f2.thenCompose(
                    value2 -> f3.thenApply(
                            value3 -> value + value2 + value3)));
  }


}
