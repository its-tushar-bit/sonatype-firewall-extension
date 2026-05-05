/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HostedRepositoryMonitoringTaskTest
    extends AbstractComponentTest
{
  @Inject
  private HostedRepositoryMonitoringTask hostedRepositoryMonitoringTask;

  @Mock
  private HostedRepositoryMonitor hostedRepositoryMonitorMock;

  @Override
  public void configure(final Binder binder) {
    binder.bind(HostedRepositoryMonitor.class).toInstance(hostedRepositoryMonitorMock);
    super.configure(binder);
  }

  @Test
  public void testExecute_delegatesToMonitor() {
    hostedRepositoryMonitoringTask.execute(mock(JobExecutionContext.class));

    verify(hostedRepositoryMonitorMock).run();
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(HostedRepositoryMonitoringTask.class)
        .build()
        .isConcurrentExectionDisallowed()).isTrue();
  }
}
