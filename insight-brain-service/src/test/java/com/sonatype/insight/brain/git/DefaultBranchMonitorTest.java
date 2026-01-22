/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.time.LocalTime;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultBranchMonitorTest
    extends AbstractComponentTest
{
  @Inject
  private DefaultBranchMonitor defaultBranchMonitor;

  @Inject
  private Configuration configuration;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private SourceControlEventPublisher sourceControlEventPublisherMock;

  @Mock
  private IqForScmLicenseChecker mockLicenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Inject
  private DefaultBranchMonitorExecutor branchMonitorExecutor;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Override
  public void configure(Binder binder) {
    lenient().when(taskSchedulerMock.isSchedulerInitialized()).thenReturn(true);
    lenient().when(taskSchedulerMock.isTaskScheduled(any())).thenReturn(true);
    lenient().when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    lenient().when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(true);

    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(SourceControlEventPublisher.class).toInstance(sourceControlEventPublisherMock);
    binder.bind(IqForScmLicenseChecker.class).toInstance(mockLicenseChecker);
    binder.bind(ApiConfigFeaturesService.class).toInstance(mockApiConfigFeaturesService);

    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(DefaultBranchMonitor.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() {
    when(mockLicenseChecker.isIqForScmSupported()).thenReturn(true);

    BranchMonitorExecutor branchMonitorExecutorMock = mock(BranchMonitorExecutor.class);
    defaultBranchMonitor.setBranchMonitorExecutor(branchMonitorExecutorMock);

    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(branchMonitorExecutorMock).performScan(any());

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitor.execute(mock(JobExecutionContext.class));
    }

    verify(branchMonitorExecutorMock).performScan(any());
  }

  @Test
  public void testExecute_Unlicensed() {
    // mockLicenseChecker.isIqForScmSupported() returns false by default

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    BranchMonitorExecutor branchMonitorExecutorSpy = spy(branchMonitorExecutor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(branchMonitorExecutorSpy, never()).performScan(any());
  }

  @Test
  public void testExecute_DefaultBranchMonitoringFeatureDisabled() {
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    BranchMonitorExecutor branchMonitorExecutorSpy = spy(branchMonitorExecutor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(branchMonitorExecutorSpy, never()).performScan(any());
  }

  @Test
  public void testExecute_SaasLifecycleSCMFeatureDisabled() {
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    BranchMonitorExecutor branchMonitorExecutorSpy = spy(branchMonitorExecutor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(branchMonitorExecutorSpy, never()).performScan(any());
  }

  @Test
  public void testStart_FeatureDisabled() {
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);
    defaultBranchMonitor.register();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(defaultBranchMonitor);
  }

  @Test
  public void testStart_FeatureEnabled() {
    defaultBranchMonitor.register();

    verify(taskSchedulerMock).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock, never()).unscheduleTask(defaultBranchMonitor);
  }

  @Test
  public void testStart_SaasLifecycleSCMFeatureDisabled() {
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);
    defaultBranchMonitor.register();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(defaultBranchMonitor);
  }

  @Test
  public void testSourceControlConfigurationChanged_NullConfiguration() {
    defaultBranchMonitor.register();

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(24),
        branchMonitorExecutor.getDefaultBranchMonitorStartTime(new SourceControlConfiguration()));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringStartTime() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(LocalTime.of(1, 11));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(24),
        branchMonitorExecutor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringIntervalHours() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(12);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(12),
        branchMonitorExecutor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_NoRelevantUpdate() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any());
  }
}
