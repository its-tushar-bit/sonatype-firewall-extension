/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BooleanSupplierShutdownRequestTest
{
  @Test
  public void testExecute() throws Exception {
    AtomicInteger atomicInteger = new AtomicInteger(3);
    BooleanSupplier item = () -> atomicInteger.getAndDecrement() != 0;
    Duration pollDuration = Duration.ofMillis(200);
    BooleanSupplierShutdownRequest spyBooleanSupplierShutdownRequest =
        spy(new BooleanSupplierShutdownRequest(item, 0, null, pollDuration));

    long start = System.currentTimeMillis();
    Thread thread = new Thread(() -> {
      try {
        Future<?> shutdown = spyBooleanSupplierShutdownRequest.execute(null);
        assertThat(atomicInteger.get()).isEqualTo(3);
        verify(spyBooleanSupplierShutdownRequest, never()).sleep(anyLong());
        shutdown.get();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    thread.start();
    thread.join(1200); // Wait at most double the expected duration
    long duration = System.currentTimeMillis() - start;

    assertThat(atomicInteger.get()).isEqualTo(-1); // It decrements 1 last time before exiting the while loop
    verify(spyBooleanSupplierShutdownRequest, times(3)).sleep(200);
    assertThat(duration).isGreaterThanOrEqualTo(600);
  }
}
