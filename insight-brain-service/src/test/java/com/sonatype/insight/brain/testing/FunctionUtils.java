/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FunctionUtils
{
  @FunctionalInterface
  public interface ConsumerWithException<T>
  {
    void accept(T t) throws Exception;
  }

  @FunctionalInterface
  public interface PredicateWithException<T>
  {
    boolean accept(T t) throws Exception;
  }

  /**
   * Wrap a Consumer-like lambda that throws checked exceptions into a proper java.util.function.Consumer that throws
   * only unchecked exceptions.
   */
  public static <T> Consumer<T> wrapException(ConsumerWithException<T> consumer) {
    return x -> {
      try {
        consumer.accept(x);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Wrap a Predicate-like lambda that throws checked exceptions into a proper java.util.function.Predicate that throws
   * only unchecked exceptions.
   */
  public static <T> Predicate<T> wrapException(PredicateWithException<T> consumer) {
    return x -> {
      try {
        return consumer.accept(x);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}
