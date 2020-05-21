/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.concurrent.Callable;

public class SystemCallable<T>
    implements Callable<T>
{
  private final Callable<T> wrapped;

  public SystemCallable(Callable<T> wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public T call() throws Exception {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      return wrapped.call();
    }
  }
}
