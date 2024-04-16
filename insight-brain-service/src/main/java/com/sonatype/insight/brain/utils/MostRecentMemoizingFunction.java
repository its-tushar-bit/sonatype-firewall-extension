/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * A function wrapper that memoizes the most recent argument that it was called with. That is, it caches the result of
 * its most recent call and will continue to return that result without calling the delegate function as long as it
 * continues to be called with the same argument. If it is called with a new argument, it will execute the delegate,
 * cache the new result, and forget the old result.
 */
public class MostRecentMemoizingFunction<T, R>
    implements Function<T, R>
{
  private final Function<T, R> delegate;

  // Store the memoized argument and return value together so we can get both of them atomically
  private volatile ImmutablePair<T, R> mostRecentArgAndRetval = null;

  public MostRecentMemoizingFunction(Function<T, R> delegate) {
    this.delegate = delegate;
  }

  @Override
  public R apply(T arg) {
    ImmutablePair<T, R> mostRecentArgAndRetval = this.mostRecentArgAndRetval;
    if (matchesArg(arg, mostRecentArgAndRetval)) {
      return mostRecentArgAndRetval.getRight();
    }
    else {
      synchronized (this) {
        mostRecentArgAndRetval = this.mostRecentArgAndRetval;
        if (matchesArg(arg, mostRecentArgAndRetval)) {
          return mostRecentArgAndRetval.getRight();
        }
        else {
          return doApply(arg);
        }
      }
    }
  }

  private static <T> boolean matchesArg(T arg, ImmutablePair<T, ?> mostRecentArgAndRetval) {
    return mostRecentArgAndRetval != null && Objects.equals(arg, mostRecentArgAndRetval.getLeft());
  }

  private synchronized R doApply(T arg) {
    R retval = delegate.apply(arg);
    mostRecentArgAndRetval = ImmutablePair.of(arg, retval);
    return retval;
  }
}
