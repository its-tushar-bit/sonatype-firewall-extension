/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted.monitoring;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class HostedRepositoryMonitorSchedulerTest
    extends AbstractComponentTest
{
  @Inject
  private HostedRepositoryMonitorScheduler scheduler;

  @Inject
  private Configuration configuration;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private HostedRepositoryMonitor hostedRepositoryMonitorMock;

  @Before
  public void applyOverrides() {
    applyBeanFieldOverride(HostedRepositoryMonitorScheduler.class, "taskScheduler", taskSchedulerMock);
    applyBeanFieldOverride(HostedRepositoryMonitorScheduler.class, "hostedRepositoryMonitorProvider",
        (jakarta.inject.Provider<HostedRepositoryMonitor>) () -> hostedRepositoryMonitorMock);
  }

  @Before
  public void enableFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    scheduler.disableForTesting = false;
  }

  @After
  public void resetFeatureFlag() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testRegister_featureFlagEnabled_schedulesTask() {
    scheduler.register();

    ArgumentCaptor<LocalTime> startTimeCaptor = ArgumentCaptor.forClass(LocalTime.class);
    verify(taskSchedulerMock).scheduleDailyTask(any(HostedRepositoryMonitoringTask.class),
        startTimeCaptor.capture());

    LocalTime base = LocalTime.of(configuration.getPolicyMonitoringHour(), 0);
    long minutesOffset = ChronoUnit.MINUTES.between(base, startTimeCaptor.getValue());
    if (minutesOffset < 0) {
      minutesOffset += 24 * 60; // handle midnight wrap-around
    }
    assertThat(minutesOffset).isBetween(0L, 119L);
  }

  @Test
  public void testRegister_featureFlagDisabled_doesNotSchedule() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    scheduler.register();

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testRegister_disableForTesting_doesNotSchedule() {
    scheduler.disableForTesting = true;

    scheduler.register();

    verify(taskSchedulerMock, never()).scheduleDailyTask(any(), any());
  }

  @Test
  public void testConfigurationChanged_featureEnabled_schedulesTask() {
    scheduler.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    verify(taskSchedulerMock).scheduleDailyTask(any(HostedRepositoryMonitoringTask.class), any());
  }

  @Test
  public void testConfigurationChanged_featureDisabled_stopsMonitoring() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    scheduler.configurationChanged(Set.of(SystemConfigurationProperty.HOSTED_REPOSITORY_EVALUATION));

    verify(taskSchedulerMock, never()).scheduleDailyTask(any(), any());
  }

  @Test
  public void testConfigurationChanged_unrelatedProperty_ignored() {
    scheduler.configurationChanged(Set.of("someOtherProperty"));

    verifyNoInteractions(taskSchedulerMock);
  }

  @Test
  public void testExecute_AdminTask_triggersImmediately() throws Exception {
    scheduler.execute(java.util.Map.of(), new PrintWriter(OutputStream.nullOutputStream()));

    verify(hostedRepositoryMonitorMock).run();
  }
}
