package se.krka.futures;

import com.spotify.futures.CompletableFuturesExtra;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class ObtrudeTest {
  @Test
  public void testObtrude() {
    CompletableFuture<String> future = CompletableFuture.completedFuture("first");
    CompletableFuture<String> future2 = future.thenApply(v -> v + " second");

    assertEquals("first", CompletableFuturesExtra.getCompleted(future));
    assertEquals("first second", CompletableFuturesExtra.getCompleted(future2));

    future.obtrudeValue("not-first");
    assertEquals("not-first", CompletableFuturesExtra.getCompleted(future));
    assertEquals("not-first second", CompletableFuturesExtra.getCompleted(future2));
  }
}
