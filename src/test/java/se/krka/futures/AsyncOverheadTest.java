package se.krka.futures;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * This test tries to measure the overhead of using Async vs not Async for transforming futures.
 */
public class AsyncOverheadTest {

  public static final int N = 10_000;
  public static final int K = 1000;

  public static void main(String[] args) {
    while (true) {
      eval("from main", true, false);
      eval("from main", true, true);
      eval("from main", false, false);
      eval("from main", false, true);

      evalFjp(true, false);
      evalFjp(true, true);
      evalFjp(false, false);
      evalFjp(false, true);

      System.out.println();

    }
  }

  private static void evalFjp(boolean completeBefore, boolean async) {
    ForkJoinPool.commonPool().submit(() -> eval("from fjp", completeBefore, async)).join();
  }

  private static void eval(String testCaseName, boolean completeBefore, boolean async) {
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < N; i++) {
      CompletableFuture<String> f = new CompletableFuture<>();
      if (completeBefore) {
        f.complete("");
      }
      CompletableFuture<String> f2 = f;
      for (int j = 0; j < K; j++) {
        if (async) {
          f2 = f2.thenApplyAsync(s -> s);
        } else {
          f2 = f2.thenApply(s -> s);
        }
      }
      if (!completeBefore) {
        f.complete("");
      }
      f2.join();
    }
    long t2 = System.currentTimeMillis();
    final long diff = t2 - t1;
  System.out.printf("%20s, async: %5s, completed before: %5s, time: %2.3f seconds%n", testCaseName, Boolean.toString(async), Boolean.toString(completeBefore), diff / 1000.0);
  }
}
