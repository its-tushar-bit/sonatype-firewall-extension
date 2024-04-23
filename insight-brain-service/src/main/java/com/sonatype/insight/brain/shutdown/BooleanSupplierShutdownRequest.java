/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

public class BooleanSupplierShutdownRequest
    extends AbstractShutdownRequest<BooleanSupplier>
{
  private static final Duration DEFAULT_POLL_DURATION = Duration.ofMillis(100);

  private final Duration pollDuration;

  public BooleanSupplierShutdownRequest(final BooleanSupplier item, final int order, final String origin) {
    this(item, order, origin, DEFAULT_POLL_DURATION);
  }

  public BooleanSupplierShutdownRequest(
      final BooleanSupplier item,
      final int order,
      final String origin,
      final Duration pollDuration)
  {
    super(item, order, origin);
    this.pollDuration = pollDuration;
  }

  @Override
  public Future<?> execute(final ExecutorService executorService) {
    return new CompleteOnGetFuture<>(() -> {
      while (getItem().getAsBoolean()) {
        sleep(pollDuration.toMillis());
      }
    });
  }

  // Visible for testing
  void sleep(final long millis) throws InterruptedException {
    Thread.sleep(millis);
  }
}
