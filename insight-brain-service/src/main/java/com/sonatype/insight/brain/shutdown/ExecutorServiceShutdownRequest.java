/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceShutdownRequest
    extends WeakReferenceShutdownRequest<ExecutorService>
{
  public ExecutorServiceShutdownRequest(
      final ExecutorService item,
      final int order,
      final String origin)
  {
    super(item, order, origin);
  }

  @Override
  public Future<?> execute(final ExecutorService executorService, final ExecutorService item) {
    item.shutdown();
    // Wait effectively forever by passing a huge timeout (since ExecutorService has no other method)
    // Timeouts in general are handled separately
    return new CompleteOnGetFuture<>(() -> item.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
  }
}
