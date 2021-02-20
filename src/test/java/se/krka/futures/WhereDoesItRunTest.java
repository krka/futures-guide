package se.krka.futures;

import com.google.common.util.concurrent.Futures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WhereDoesItRunTest {

  @Test
  public void testExecutor() {
    ExecutorService runner = Util.newExecutor("runner");
    ExecutorService banana = Util.newExecutor("banana");
    ExecutorService apple = Util.newExecutor("apple");

    ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    IntStream.range(0, 1000000).parallel().mapToObj(i ->
            runner.submit(
                    () -> {
                      CompletableFuture<Void> voidCompletableFuture = CompletableFuture
                              .supplyAsync(() -> Util.currThread(), banana)
                              .thenApplyAsync(s -> s + " -> " + Util.currThread(), apple)
                              .thenApply(s -> s + " -> " + Util.currThread())
                              .thenAccept(s -> counters.computeIfAbsent(s, key -> new AtomicInteger()).incrementAndGet());
                      return voidCompletableFuture;
                    }
            ))
            .collect(Collectors.toList())
            .stream().map(Futures::getUnchecked)
            .forEach(CompletableFuture::join);

    // Expected output:
    //    999180: banana -> apple -> apple
    //       820: banana -> apple -> runner

    counters.forEach((s, count) -> System.out.printf("%10d: %s\n", count.get(), s));
  }

  @Test
  public void testExecutorWithException() {
    ExecutorService runner = Util.newExecutor("runner");
    ExecutorService banana = Util.newExecutor("banana");
    ExecutorService apple = Util.newExecutor("apple");

    ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    IntStream.range(0, 1000000).parallel().mapToObj(i ->
            runner.submit(
                    () -> CompletableFuture
                            .supplyAsync(() -> Util.currThread(), banana)
                            .thenApply(s -> Util.doThrow(new RuntimeException(s + " -> " + Util.currThread())))
                            .thenApplyAsync(s -> s + " -> " + Util.currThread(), apple)
                            .exceptionally(e -> e.getMessage() + " -> " + Util.currThread())
                            .thenAccept(s -> counters.computeIfAbsent(s, key -> new AtomicInteger()).incrementAndGet())
            ))
            .collect(Collectors.toList())
            .stream().map(Futures::getUnchecked)
            .forEach(CompletableFuture::join);

    // Note that nothing ever runs on "apple"!

    // Expected output:
    //         1: java.lang.RuntimeException: banana -> banana -> runner
    //    999997: java.lang.RuntimeException: banana -> banana -> banana
    //         2: java.lang.RuntimeException: banana -> runner -> runner

    counters.forEach((s, count) -> System.out.printf("%10d: %s\n", count.get(), s));
  }

  @Test
  public void testExecutorWithExceptionAndHandle() {
    ExecutorService runner = Util.newExecutor("runner");
    ExecutorService banana = Util.newExecutor("banana");
    ExecutorService apple = Util.newExecutor("apple");

    ConcurrentMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    IntStream.range(0, 1000000).parallel().mapToObj(i ->
            runner.submit(
                    () -> CompletableFuture
                            .supplyAsync(() -> Util.currThread(), banana)
                            .thenApply(s -> Util.<String>doThrow(new RuntimeException(s + " -> " + Util.currThread())))
                            .handleAsync((s, ex) -> ex.getMessage() + " -> " + Util.currThread(), apple)
                            .exceptionally(e -> e.getMessage() + " -> " + Util.currThread())
                            .thenAccept(s -> counters.computeIfAbsent(s, key -> new AtomicInteger()).incrementAndGet())
            ))
            .collect(Collectors.toList())
            .stream().map(Futures::getUnchecked)
            .forEach(CompletableFuture::join);

    // Expected output:
    //    1000000: java.lang.RuntimeException: banana -> banana -> apple

    counters.forEach((s, count) -> System.out.printf("%10d: %s\n", count.get(), s));
  }

}
