package se.krka.futures;

import com.spotify.futures.CompletableFutures;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Util {
  static String currThread() {
    return Thread.currentThread().getName();
  }

  static ExecutorService newExecutor(String name) {
    return Executors.newSingleThreadExecutor(
            runnable -> {
              Thread thread = new Thread(runnable);
              thread.setDaemon(true);
              thread.setName(name);
              return thread;
            });
  }

  static <T> T doThrow(RuntimeException ex) {
    throw ex;
  }

  static void assertException(CompletableFuture<?> input, List<Class<? extends Throwable>> expected) {
    assertEquals(expected, toList(getException(input)));
  }

  private static Throwable getException(CompletableFuture<?> input) {
    try {
      input.join();
      fail();
      throw new RuntimeException("unreachable");
    } catch (Exception e) {
      return e;
    }
  }

  static List<Class<?>> toList(Throwable throwable) {
    Class<?> clazz = throwable.getClass();
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return List.of(clazz);
    }
    Class<?> causeClass = cause.getClass();
    return List.of(clazz, causeClass);
  }

  static Throwable exceptionFromCallback(CompletableFuture<?> future) {
    assertTrue(future.isDone());
    AtomicReference<Throwable> exception = new AtomicReference<>();
    future.exceptionally(e -> { exception.set(e); return null; });
    return exception.get();
  }
}
