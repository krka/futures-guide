package se.krka.futures;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListAPI {
  public static void main(String[] args) {
    System.out.println("Static methods:");
    printMethods(method -> Modifier.isStatic(method.getModifiers()));
    System.out.println();
    System.out.println("Instance methods:");
    printMethods(method -> !Modifier.isStatic(method.getModifiers()));
  }

  private static void printMethods(Predicate<Method> filter) {
    Stream.of(CompletableFuture.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(filter)
            .map(method -> "* `" + pretty(method) + "` - ")
            .sorted()
            .distinct()
            .forEach(System.out::println);
  }

  private static String pretty(Method method) {
    return method.getName() + "(" + pretty(method.getParameterTypes()) + ")";
  }

  private static String pretty(Class<?>[] parameterTypes) {
    return List.of(parameterTypes).stream().map(ListAPI::pretty).collect(Collectors.joining(", "));
  }

  private static String pretty(Class<?> type) {
    if (type.isArray()) {
      return pretty(type.getComponentType()) + "[]";
    }
    if (type.isPrimitive()) {
      return type.getName();
    }
    return type.getName();
  }
}
