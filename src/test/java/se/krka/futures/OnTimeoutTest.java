package se.krka.futures;

import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OnTimeoutTest {
  @Test
  public void testTimeoutNotBlocking() throws Exception {
    assertTrue(measureTimeout() < 200);
  }

  @Test
  public void testTimeoutNotBlockingTwice() throws Exception {
    assertTrue(measureTimeout() < 200);
    assertTrue(measureTimeout() < 200);
  }

  @Test
  public void testTimeoutBlocking() throws Exception {
    try (Killer killer = new Killer(killTimeout())) {
      expectFailure();
    }
  }

  @Test
  public void testTimeoutBlocking2() throws Exception {
    try (Killer killer = new Killer(killTimeout2())) {
      expectFailure();
    }
  }

  private void expectFailure() throws Exception {
    long time = measureTimeout();
    assertTrue("time was " + time, time >= 1000);
  }

  private long measureTimeout() throws InterruptedException, TimeoutException {
    CompletableFuture<String> timeoutFuture = new CompletableFuture<String>().orTimeout(1, TimeUnit.MILLISECONDS);

    long t1 = System.currentTimeMillis();
    try {
      timeoutFuture.get(10, TimeUnit.SECONDS);
      throw new AssertionError("Unreachable");
    } catch (ExecutionException e) {
      assertEquals(TimeoutException.class, e.getCause().getClass());
      long t2 = System.currentTimeMillis();
      return t2 - t1;
    }
  }

  private CompletableFuture<String> killTimeout() {
    return new CompletableFuture<String>()
            .orTimeout(1, TimeUnit.MILLISECONDS)
            .handle((s, t) -> {
              try {
                sleepOnThread();
                return "";
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            })
            .exceptionally(Throwable::getMessage);
  }

  private CompletableFuture<String> killTimeout2() {
    return new CompletableFuture<String>()
            .completeOnTimeout("", 1, TimeUnit.MILLISECONDS)
            .thenApply(s -> {
              try {
                sleepOnThread();
                return s;
              } catch (InterruptedException e) {
                return s;
              }
            });
  }

  private void sleepOnThread() throws InterruptedException {
    int millis = 1000;
    System.out.println("Sleeping on " + Util.currThread() + " for " + millis + " ms");
    Thread.sleep(millis);
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
