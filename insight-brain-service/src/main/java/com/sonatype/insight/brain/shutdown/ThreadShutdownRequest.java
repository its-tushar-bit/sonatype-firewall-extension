/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ThreadShutdownRequest
    extends WeakReferenceShutdownRequest<Thread>
{
  public ThreadShutdownRequest(final Thread item, final int order, final String origin) {
    super(item, order, origin);
  }

  @Override
  public Future<?> execute(final ExecutorService executorService, final Thread item) {
    return new CompleteOnGetFuture<>(item::join);
  }
}
