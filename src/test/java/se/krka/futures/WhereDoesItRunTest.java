package se.krka.futures;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class WhereDoesItRunTest {
  @Test
  public void testExecutor() {
    ExecutorService runner = Util.newExecutor("runner");
    ExecutorService banana = Util.newExecutor("banana");
    ExecutorService apple = Util.newExecutor("apple");

    ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    IntStream.range(0, 10000000).parallel().forEach(i -> {
      runner.submit(
              () -> CompletableFuture
                      .supplyAsync(() -> Util.currThread(), banana)
                      .thenApplyAsync(s -> s + " -> " + Util.currThread())
                      .thenApply(s -> s + " -> " + Util.currThread())
                      .thenAccept(s -> counters.computeIfAbsent(s, key -> new AtomicInteger()).incrementAndGet())
      );
    });

    // Expected output:
    //   1103885: banana -> apple -> apple
    //        47: banana -> apple -> runner

    counters.forEach((s, count) -> System.out.printf("%10d: %s\n", count.get(), s));
  }

  @Test
  public void testAasd() {
    CompletableFuture<Object> future = new CompletableFuture<>();
    //CompletableFuture<Object> future2 = handleCompose(future, executor);
  }

  private CompletableFuture<Object> handleCompose(CompletableFuture<Object> future, Executor executor) {
    return future.handle((o, throwable) -> CompletableFuture.completedFuture(o))
            .thenComposeAsync(x -> x, executor);
  }
}
