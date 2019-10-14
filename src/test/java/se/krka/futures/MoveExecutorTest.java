package se.krka.futures;

import org.junit.Test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

public class MoveExecutorTest {
  @Test
  public void testMoveExecutor() {
    ExecutorService first = Util.newExecutor("first");
    ExecutorService second = Util.newExecutor("second");
    CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> Util.currThread(), first)
            .thenApplyAsync(s -> Util.currThread(), second);
    assertTrue(Set.of("second", "main").contains(future.join()));
  }

  @Test
  public void testMoveExecutorWithException() {
    ExecutorService first = Util.newExecutor("first");
    ExecutorService second = Util.newExecutor("second");
    CompletableFuture<String> future = CompletableFuture
            .<String>supplyAsync(() -> Util.doThrow(new RuntimeException(Util.currThread())), first)
            .thenApplyAsync(Function.identity(), second)
            .exceptionally(throwable -> Util.currThread());
    assertTrue(Set.of("first", "main").contains(future.join()));
  }

  @Test
  public void testMoveExecutorWithExceptionActuallyMove() {
    ExecutorService first = Util.newExecutor("first");
    ExecutorService second = Util.newExecutor("second");
    CompletableFuture<String> future = CompletableFuture
            .<String>supplyAsync(() -> Util.doThrow(new RuntimeException(Util.currThread())), first)
            .whenCompleteAsync((s, t) -> {}, second)
            .exceptionally(throwable -> Util.currThread());
    assertTrue(Set.of("second", "main").contains(future.join()));
  }
}
