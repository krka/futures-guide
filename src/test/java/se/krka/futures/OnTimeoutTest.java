package se.krka.futures;

import org.junit.Test;

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
    testTimeout();
  }

  @Test
  public void testTimeoutNotBlockingTwice() throws Exception {
    testTimeout();
    testTimeout();
  }

  @Test
  public void testTimeoutBlocking() throws Exception {
    CompletableFuture<String> killTimeout = killTimeout();

    Thread.sleep(10);
    try {
      testTimeout();
      fail("Expected a timeout exception");
    } catch (TimeoutException e) {
      // expected case
    }

    System.out.println(killTimeout.join());
  }

  private void testTimeout() throws InterruptedException, TimeoutException {
    CompletableFuture<String> timeoutFuture = new CompletableFuture<String>().orTimeout(1, TimeUnit.MILLISECONDS);

    long t1 = System.currentTimeMillis();
    try {
      timeoutFuture.get(1, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      assertEquals(TimeoutException.class, e.getCause().getClass());
      long t2 = System.currentTimeMillis();
      long diff = t2 - t1;
      assertTrue(diff < 900);
    }
  }

  private CompletableFuture<String> killTimeout() {
    return new CompletableFuture<String>()
            .orTimeout(1, TimeUnit.MILLISECONDS)
            .handle((s, t) -> {
              try {
                System.out.println("Sleeping on " + Thread.currentThread().getName());
                Thread.sleep(1000);
                System.out.println("Done sleeping on " + Thread.currentThread().getName());
                return "";
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            })
            .exceptionally(Throwable::getMessage);
  }
}
