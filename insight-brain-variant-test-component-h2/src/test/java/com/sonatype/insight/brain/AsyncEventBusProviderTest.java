/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class AsyncEventBusProviderTest
    extends AbstractComponentH2Test
{
  @Mock
  private Configuration mockConfiguration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Test
  public void testAsyncEventBusProvider_AddsExecutorToShutdownHandler() {
    when(mockConfiguration.getEventBusMaxThreadPoolSize()).thenReturn(AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);
    AsyncEventBusProvider asyncEventBusProvider = new AsyncEventBusProvider(mockConfiguration, mockShutdownHandler);

    assertThat(asyncEventBusProvider.get().getMaxPoolSize()).isEqualTo(AsyncEventBus.DEFAULT_MAX_POOL_SIZE + 1);
    verify(mockShutdownHandler).add(asyncEventBusProvider.get().getThreadPoolExecutor(),
        ShutdownPriority.ASYNC_EVENT_BUS);
  }
}
