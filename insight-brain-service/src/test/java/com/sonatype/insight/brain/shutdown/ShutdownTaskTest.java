/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.sonatype.insight.brain.service.Configuration;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ShutdownTaskTest
{
  @Mock
  private Configuration mockConfiguration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private ShutdownTask shutdownTask;

  @BeforeEach
  public void before() {
    shutdownTask = new ShutdownTask(mockConfiguration, mockShutdownHandler);
  }

  @Test
  public void testGetPath() {
    assertThat(shutdownTask.getPath()).isEqualTo("shutdown");
  }

  @Test
  public void testExecute_SkipDefaultShouldNotExit() throws Exception {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(60);

    shutdownTask.execute(Collections.emptyMap(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockShutdownHandler).trigger(Duration.ofSeconds(660), false);
  }

  @Test
  public void testExecute_ShouldSkipExitIsFalse() throws Exception {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(60);

    shutdownTask.execute(Map.of(ShutdownTask.SKIP_SYSTEM_EXIT_QUERY_PARAM, Lists.newArrayList("false")),
        new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockShutdownHandler).trigger(Duration.ofSeconds(660), false);
  }

  @Test
  public void testExecute_ShouldSkipExitIsTrue() throws Exception {
    when(mockConfiguration.getReportTimeoutInSeconds()).thenReturn(60);

    shutdownTask.execute(Map.of(ShutdownTask.SKIP_SYSTEM_EXIT_QUERY_PARAM, Lists.newArrayList("true")),
        new PrintWriter(OutputStream.nullOutputStream()));

    verify(mockShutdownHandler).trigger(Duration.ofSeconds(660), true);
  }
}
