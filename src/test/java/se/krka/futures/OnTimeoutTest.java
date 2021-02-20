package se.krka.futures;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OnTimeoutTest {
  @Test
  public void testTimeoutNotBlocking() throws Exception {
    waitForNotBlocking();
    assertTrue(measureTimeout() < 200);
  }

  @Test
  public void testTimeoutNotBlockingTwice() throws Exception {
    waitForNotBlocking();
    assertTrue(measureTimeout() < 200);
    assertTrue(measureTimeout() < 200);
  }

  @Test
  public void testTimeoutBlocking() throws Exception {
    waitForNotBlocking();
    try (Killer killer = new Killer(killTimeout())) {
      expectFailure();
    }
  }

  @Test
  public void testTimeoutBlocking2() throws Exception {
    waitForNotBlocking();
    try (Killer killer = new Killer(killTimeout2())) {
      expectFailure();
    }
  }

  @Test
  public void testTimeoutBlockingWithDelayedExecutor() throws Exception {
    waitForNotBlocking();
    try (Killer killer = new Killer(killTimeout3())) {
      expectFailure();
    }
  }

  private void expectFailure() throws Exception {
    long time = measureTimeout();
    assertTrue("time was " + time, time >= 900);
  }

  private long measureTimeout() throws InterruptedException, TimeoutException {
    CompletableFuture<String> timeoutFuture = new CompletableFuture<String>().orTimeout(1, TimeUnit.MILLISECONDS);

    long t1 = System.currentTimeMillis();
    try {
      timeoutFuture.get(10, TimeUnit.SECONDS);
      throw new AssertionError("Unreachable");
    } catch (ExecutionException e) {
      assertEquals(TimeoutException.class, Util.exceptionFromCallback(timeoutFuture).getClass());
      assertEquals(TimeoutException.class, e.getCause().getClass());
      long t2 = System.currentTimeMillis();
      return t2 - t1;
    }
  }

  private CompletableFuture<String> killTimeout() {
    waitForNotBlocking();
    CompletableFuture<String> future = new CompletableFuture<String>()
            .orTimeout(10, TimeUnit.MILLISECONDS)
            .exceptionally(Throwable::getMessage)
            .thenApply(s -> sleepOnThread(s, 1000));
    waitForBlocking();
    return future;
  }

  private CompletableFuture<String> killTimeout2() {
    CompletableFuture<String> future = new CompletableFuture<String>()
            .completeOnTimeout("", 10, TimeUnit.MILLISECONDS)
            .thenApply(s -> sleepOnThread(s, 1000));
    waitForBlocking();
    return future;
  }

  private CompletableFuture<String> killTimeout3() {
    Executor executor = CompletableFuture.delayedExecutor(1, TimeUnit.MILLISECONDS, MoreExecutors.directExecutor());
    CompletableFuture<String> future = new CompletableFuture<>();
    executor.execute(() -> {
      sleepOnThread(null, 1000);
      future.complete("value");
    });
    waitForBlocking();
    return future;
  }

  private static final AtomicBoolean BLOCKING_COMPLETABLE_FUTURE_DELAY_SCHEDULER = new AtomicBoolean(false);

  private <T> T sleepOnThread(T value, int millis) {
    if (Util.currThread().equals("CompletableFutureDelayScheduler")) {
      BLOCKING_COMPLETABLE_FUTURE_DELAY_SCHEDULER.set(true);
    }
    try {
      System.out.println("Sleeping on " + Util.currThread() + " for " + millis + " ms");
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (Util.currThread().equals("CompletableFutureDelayScheduler")) {
        BLOCKING_COMPLETABLE_FUTURE_DELAY_SCHEDULER.set(false);
      }
    }
    return value;
  }

  private void waitForBlocking() {
    while (!BLOCKING_COMPLETABLE_FUTURE_DELAY_SCHEDULER.get()) {
      Thread.yield();
    }
  }

  private void waitForNotBlocking() {
    while (BLOCKING_COMPLETABLE_FUTURE_DELAY_SCHEDULER.get()) {
      Thread.yield();
    }
  }

  private static class Killer implements Closeable {

    private final CompletableFuture<String> future;

    private Killer(CompletableFuture<String> future) {
      this.future = future;
    }

    @Override
    public void close() {
      future.join();
    }
  }
}
