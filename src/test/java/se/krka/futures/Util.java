package se.krka.futures;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Util {
  static String currThread() {
    return Thread.currentThread().getName();
  }

  static ExecutorService newExecutor(String name) {
    return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(name).build());
  }

  static <T> T doThrow(RuntimeException ex) {
    throw ex;
  }

  static void assertException(CompletableFuture<?> input, ImmutableList<Class<? extends Throwable>> expected) {
    try {
      input.join();
      fail();
    } catch (Exception e) {
      assertEquals(expected, toList(e));
    }
  }

  static ImmutableList<Class<?>> toList(Throwable throwable) {
    Class<?> clazz = throwable.getClass();
    Throwable cause = throwable.getCause();
    if (cause == null) {
      return ImmutableList.of(clazz);
    }
    Class<?> causeClass = cause.getClass();
    return ImmutableList.of(clazz, causeClass);
  }
}
