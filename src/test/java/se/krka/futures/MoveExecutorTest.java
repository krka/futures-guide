package se.krka.futures;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

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
    assertTrue(ImmutableSet.of("second", "main").contains(future.join()));
  }

  @Test
  public void testMoveExecutorWithException() {
    ExecutorService first = Util.newExecutor("first");
    ExecutorService second = Util.newExecutor("second");
    CompletableFuture<String> future = CompletableFuture
            .<String>supplyAsync(() -> Util.doThrow(new RuntimeException(Util.currThread())), first)
            .thenApplyAsync(Function.identity(), second)
            .exceptionally(throwable -> Util.currThread());
    assertTrue(ImmutableSet.of("first", "main").contains(future.join()));
  }

  @Test
  public void testMoveExecutorWithExceptionActuallyMove() {
    ExecutorService first = Util.newExecutor("first");
    ExecutorService second = Util.newExecutor("second");
    CompletableFuture<String> future = CompletableFuture
            .<String>supplyAsync(() -> Util.doThrow(new RuntimeException(Util.currThread())), first)
            .whenCompleteAsync((s, t) -> {}, second)
            .exceptionally(throwable -> Util.currThread());
    assertTrue(ImmutableSet.of("second", "main").contains(future.join()));
  }
}
