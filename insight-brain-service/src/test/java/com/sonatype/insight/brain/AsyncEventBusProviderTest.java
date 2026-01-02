/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;

import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(SlowTest.class)
public class AsyncEventBusProviderTest
    extends AbstractComponentTest
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
