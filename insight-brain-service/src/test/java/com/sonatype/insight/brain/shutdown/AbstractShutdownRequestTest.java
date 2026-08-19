/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractShutdownRequestTest
{
  private final ShutdownRequest<String> shutdownRequest = new AbstractShutdownRequest<>("test", 1, null)
  {
    @Override
    public Future<?> execute(final ExecutorService executorService) {
      return CompletableFuture.completedFuture(null);
    }
  };

  @Test
  public void testGetItem() {
    assertThat(shutdownRequest.getItem()).isEqualTo("test");
  }

  @Test
  public void testGetOrder() {
    assertThat(shutdownRequest.getOrder()).isEqualTo(1);
  }
}
