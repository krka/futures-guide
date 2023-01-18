package se.krka.futures;

import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class SimpleFuture<T> {
  private final AtomicReference<Result<T>> ref = new AtomicReference<>(null);
  private final Queue<Consumer<Result<T>>> callbacks = new LinkedBlockingQueue<>();

  public SimpleFuture() {
  }

  public void cancel() {
    completeExceptionally(new CancellationException());
  }

  public boolean isDone() {
    return ref.get() != null;
  }

  public T join() throws InterruptedException, ExecutionException {
    CountDownLatch latch = new CountDownLatch(1);
    addCallback(ignore -> latch.countDown());
    latch.await();
    final Result<T> result = ref.get();
    if (result.throwable != null) {
      throw new ExecutionException(result.throwable);
    } else {
      return result.value;
    }
  }

  public static <T> SimpleFuture<T> completed(T value) {
    final SimpleFuture<T> future = new SimpleFuture<>();
    future.complete(value);
    return future;
  }

  public static <T> SimpleFuture<T> exceptionallyCompleted(Throwable t) {
    final SimpleFuture<T> future = new SimpleFuture<>();
    future.completeExceptionally(t);
    return future;
  }

  public void complete(T value) {
    complete(Result.value(value));
  }

  public void completeExceptionally(Throwable t) {
    complete(Result.exception(t));
  }

  public void extrude(T value) {
    extrude(Result.value(value));
  }

  public void extrudeExceptionally(Throwable t) {
    extrude(Result.exception(t));
  }

  private void extrude(Result<T> newValue) {
    complete(newValue);
    if (ref.get() != newValue) {
      ref.set(newValue);
    }
  }

  public <U> SimpleFuture<U> thenApply(Function<T, U> transform) {
    return map(result -> result.mapValue(transform));
  }

  public <U> SimpleFuture<U> thenApplyAsync(Function<T, U> transform, Executor executor) {
    return mapAsync(result -> result.mapValue(transform), executor);
  }

  public <U> SimpleFuture<U> thenApplyAsync(Function<T, U> transform) {
    return mapAsync(result -> result.mapValue(transform));
  }

  public <U> SimpleFuture<U> thenCompose(Function<T, SimpleFuture<U>> transform) {
    final SimpleFuture<U> future = new SimpleFuture<>();
    addCallback(res -> {
      final Result<SimpleFuture<U>> composed = res.mapValue(transform);
      if (composed.value != null) {
        composed.value.addCallback(future::complete);
      } else {
        future.completeExceptionally(composed.throwable);
      }
    });
    return future;
  }

  private <U> SimpleFuture<U> map(Function<Result<T>, Result<U>> transform) {
    final SimpleFuture<U> newFuture = new SimpleFuture<>();
    addCallback(result -> newFuture.complete(result.map(transform)));
    return newFuture;
  }

  private <U> SimpleFuture<U> mapAsync(Function<Result<T>, Result<U>> transform, Executor executor) {
    final SimpleFuture<U> newFuture = new SimpleFuture<>();
    addCallback(result -> executor.execute(() -> newFuture.complete(result.map(transform))));
    return newFuture;
  }

  private <U> SimpleFuture<U> mapAsync(Function<Result<T>, Result<U>> transform) {
    return mapAsync(transform, ForkJoinPool.commonPool());
  }

  private <U> SimpleFuture<U> flatmap(Function<Result<T>, SimpleFuture<U>> transform) {
    final SimpleFuture<U> newFuture = new SimpleFuture<>();
    addCallback(result1 -> {
      final SimpleFuture<U> composedFuture = transform.apply(result1);
      if (composedFuture == null) {
        newFuture.completeExceptionally(new NullPointerException());
      } else {
        composedFuture.addCallback(newFuture::complete);
      }
    });
    return newFuture;
  }

  private <U> SimpleFuture<U> flatmapAsync(Function<Result<T>, SimpleFuture<U>> transform, Executor executor) {
    final SimpleFuture<U> newFuture = new SimpleFuture<>();
    addCallback(result1 -> {
      final SimpleFuture<U> composedFuture = transform.apply(result1);
      if (composedFuture == null) {
        newFuture.completeExceptionally(new NullPointerException());
      } else {
        composedFuture.addCallback(newFuture::complete, executor);
      }
    }, executor);
    return newFuture;
  }

  private void addCallback(Consumer<Result<T>> callback) {
    final Result<T> result = ref.get();
    if (result != null) {
      callback(callback, result);
    } else {
      callbacks.add(callback);
      if (ref.get() != null) {
        drainCallbacks();
      }
    }
  }

  private void addCallback(Consumer<Result<T>> callback, Executor executor) {
    addCallback(result -> executor.execute(() -> callback.accept(result)));
  }

  private void callback(Consumer<Result<T>> callback, Result<T> result) {
    try {
      callback.accept(result);
    } catch (Throwable e) {
      callback.accept(Result.exception(e));
    }
  }

  private void drainCallbacks() {
    final Result<T> result = ref.get();
    while (true) {
      final Consumer<Result<T>> callback = callbacks.poll();
      if (callback == null) {
        break;
      }
      callback.accept(result);
    }
  }

  private void complete(Result<T> result) {
    if (ref.compareAndSet(null, result)) {
      drainCallbacks();
    }
  }

  private static class Result<T> {
    private final T value;
    private final Throwable throwable;

    private Result(T value, Throwable throwable) {
      this.value = value;
      this.throwable = throwable;
    }

    public static <T> Result<T> value(T value) {
      return new Result<>(value, null);
    }

    public static <T> Result<T> exception(Throwable t) {
      return new Result<>(null, t);
    }

    public <U> Result<U> mapValue(Function<T, U> transform) {
      if (throwable == null) {
        try {
          return Result.value(transform.apply(value));
        } catch (Exception e) {
          return Result.exception(e);
        }
      }
      return (Result<U>) this;
    }

    public <U> Result<U> mapException(Function<Throwable, U> transform) {
      if (throwable != null) {
        try {
          return Result.value(transform.apply(throwable));
        } catch (Exception e) {
          return Result.exception(e);
        }
      }
      return (Result<U>) this;
    }

    public <U> Result<U> map(Function<Result<T>, Result<U>> transform) {
      try {
        return transform.apply(this);
      } catch (Throwable e) {
        return Result.exception(e);
      }
    }
  }
}
