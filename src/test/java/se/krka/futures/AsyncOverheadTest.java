package se.krka.futures;

import java.util.concurrent.CompletableFuture;

/**
 * This test tries to measure the overhead of using Async vs not Async for transforming futures.
 */
public class AsyncOverheadTest {

  public static final int N = 10_000;
  public static final int K = 1000;

  public static void main(String[] args) {
    while (true) {
      {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
          CompletableFuture<String> f2 = CompletableFuture.completedFuture("");
          for (int j = 0; j < K; j++) {
            f2 = f2.thenApply(s -> s);
          }
          f2.join();
        }
        long t2 = System.currentTimeMillis();
        final long diff = t2 - t1;
        System.out.println("completed, not async: " + diff / 1000.0 + " seconds");
      }

      {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
          CompletableFuture<String> f2 = CompletableFuture.completedFuture("");
          for (int j = 0; j < K; j++) {
            f2 = f2.thenApplyAsync(s -> s);
          }
          f2.join();
        }
        long t2 = System.currentTimeMillis();
        final long diff = t2 - t1;
        System.out.println("completed, async: " + diff / 1000.0 + " seconds");
      }

      {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
          final CompletableFuture<String> f = new CompletableFuture<>();
          CompletableFuture<String> f2 = f;
          for (int j = 0; j < K; j++) {
            f2 = f2.thenApply(s -> s);
          }
          f.complete("");
          f2.join();
        }
        long t2 = System.currentTimeMillis();
        final long diff = t2 - t1;
        System.out.println("not completed, not async: " + diff / 1000.0 + " seconds");
      }

      {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
          final CompletableFuture<String> f = new CompletableFuture<>();
          CompletableFuture<String> f2 = f;
          for (int j = 0; j < K; j++) {
            f2 = f2.thenApplyAsync(s -> s);
          }
          f.complete("");
          f2.join();
        }
        long t2 = System.currentTimeMillis();
        final long diff = t2 - t1;
        System.out.println("not completed, async: " + diff / 1000.0 + " seconds");
      }

      System.out.println();

    }
  }
}
