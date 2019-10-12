package se.krka.futures;

import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class WhereDoesItRunTest {
  @Test
  public void testExecutor() {
    ExecutorService runner = Util.newExecutor("runner");
    ExecutorService banana = Util.newExecutor("banana");
    ExecutorService apple = Util.newExecutor("apple");
    ConcurrentMap<String, AtomicInteger> counters = Maps.newConcurrentMap();
    IntStream.range(0, 10000000).parallel().forEach(i -> {
      runner.submit(
              () -> CompletableFuture
                      .supplyAsync(() -> Util.currThread(), banana)
                      .thenApplyAsync(s -> s + " -> " + Util.currThread(), apple)
                      .thenApply(s -> s + " -> " + Util.currThread())
                      .thenAccept(s -> counters.computeIfAbsent(s, key -> new AtomicInteger()).incrementAndGet())
      );
    });
    counters.forEach((s, count) -> System.out.printf("%10d: %s\n", count.get(), s));
  }

}
