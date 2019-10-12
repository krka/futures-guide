package se.krka.futures;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
}
