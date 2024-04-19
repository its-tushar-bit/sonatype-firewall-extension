/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import java.time.Duration;

import com.sonatype.insight.brain.service.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShutdownTaskTest
{
  @Mock
  private Configuration mockConfiguration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private ShutdownTask shutdownTask;

  @Before
  public void before() {
    shutdownTask = new ShutdownTask(mockConfiguration, mockShutdownHandler);
  }

  @Test
  public void testGetName() {
    assertThat(shutdownTask.getName()).isEqualTo("shutdown");
  }

  @Test
  public void testExecute() throws Exception {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(60);

    shutdownTask.execute(null, null);

    verify(mockShutdownHandler).trigger(Duration.ofSeconds(660));
  }
}
