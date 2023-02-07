/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlConfigurationDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.GitImplementation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.slf4j.MDC;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
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
  private DefaultIqForScmLicenseChecker mockLicenseChecker;

  @Inject
  private SourceControlConfigurationDAO sourceControlConfigurationDAO;

  @Override
  public void configure(Binder binder) {
    lenient().when(taskSchedulerMock.isSchedulerInitialized()).thenReturn(true);
    lenient().when(taskSchedulerMock.isTaskScheduled(DefaultBranchMonitor.TASK_NAME)).thenReturn(true);
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(SourceControlEventPublisher.class).toInstance(sourceControlEventPublisherMock);
    binder.bind(DefaultIqForScmLicenseChecker.class).toInstance(mockLicenseChecker);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(DefaultBranchMonitor.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() {
    when(mockLicenseChecker.isIqForScmSupported()).thenReturn(true);

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    doAnswer(invocationOnMock -> {
      assertThat(MDC.get(MDCUsernameScope.USERNAME)).isEqualTo(MDCUsernameScope.SYSTEM);
      return null;
    }).when(defaultBranchMonitorSpy).updateDefaultBranchScans();

    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(defaultBranchMonitorSpy).updateDefaultBranchScans();
  }

  @Test
  public void testExecute_Unlicensed() {
    // mockLicenseChecker.isIqForScmSupported() returns false by default

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(defaultBranchMonitorSpy, never()).updateDefaultBranchScans();
  }

  @Test
  public void testStart_FeatureEnabled() {
    Date expectedStartTime = defaultBranchMonitor.getDefaultBranchMonitorStartTime(new SourceControlConfiguration());
    defaultBranchMonitor.register();

    verify(taskSchedulerMock).schedulePeriodicTask(DefaultBranchMonitor.class, DefaultBranchMonitor.TASK_NAME,
        Duration.ofHours(12),
        expectedStartTime);
  }

  @Test
  public void testGetDefaultBranchMonitorStartTime_NotNull_DoesNotPlusRandomMinutes() {
    DefaultBranchMonitor spy = spy(defaultBranchMonitor);
    lenient().when(spy.getRandomizedStartOffsetInMinutes()).thenReturn(5);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString("00:00");

    assertThat(spy.getDefaultBranchMonitorStartTime(sourceControlConfiguration)).hasMinute(0);
  }

  @Test
  public void testGetDefaultBranchMonitorStartTime_Null_UsesDefaultPlusRandomMinutes() {
    DefaultBranchMonitor spy = spy(defaultBranchMonitor);
    lenient().when(spy.getRandomizedStartOffsetInMinutes()).thenReturn(5);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString(null);

    assertThat(spy.getDefaultBranchMonitorStartTime(sourceControlConfiguration)).hasMinute(5);
  }

  @Test
  public void testStart_FeatureDisabled() {
    SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING.setEnabled(false);
    defaultBranchMonitor.register();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(DefaultBranchMonitor.TASK_NAME);
  }

  @Test
  public void testUpdateDefaultBranchScans_applicationToUpdateScan() throws Exception {
    // given: application with outdated scan
    // Service started to initialize interval
    defaultBranchMonitor.start();
    Application app = tempEntity.newApplicationWithParent();
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusMinutes(defaultBranchMonitor.getIntervalInMinutes() + 60));
    SourceControl scRoot = tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null,
        SourceControlProvider.GITLAB);
    SourceControl sc = tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: detecting and updating default branch with outdated source scans
    defaultBranchMonitor.updateDefaultBranchScans();

    // then: source control event is published with proper source control information
    sc.setProvider(scRoot.getProvider());
    verifySourceControlEventWasSent(sc);
  }

  @Test
  public void testUpdateDefaultBranchScans_noApplicationsToScan() throws Exception {
    // given: application without scan
    // Service started to initialize interval
    defaultBranchMonitor.start();
    Application app = tempEntity.newApplicationWithParent();
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusMinutes(defaultBranchMonitor.getIntervalInMinutes() - 60));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: detecting and updating default branch with outdated source scans
    defaultBranchMonitor.updateDefaultBranchScans();

    // then: no source control event is sent
    verify(sourceControlEventPublisherMock, never()).publishEvent(any());
  }

  @Test
  public void testSourceControlConfigurationChanged_NullConfiguration() {
    defaultBranchMonitor.register();

    verify(taskSchedulerMock).schedulePeriodicTask(DefaultBranchMonitor.class, DefaultBranchMonitor.TASK_NAME,
        Duration.ofHours(12),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(new SourceControlConfiguration()));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringStartTime() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(LocalTime.of(1, 11));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(DefaultBranchMonitor.class, DefaultBranchMonitor.TASK_NAME,
        Duration.ofHours(12),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringIntervalHours() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(12);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(DefaultBranchMonitor.class, DefaultBranchMonitor.TASK_NAME,
        Duration.ofHours(6),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_NoRelevantUpdate() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any(), any());
  }

  private Date toDate(LocalDateTime localDateTime) {
    return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private void verifySourceControlEventWasSent(SourceControl sourceControl) {
    ArgumentCaptor<SourceControlEvent> sourceControlEventArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(sourceControlEventPublisherMock).publishEvent(sourceControlEventArgumentCaptor.capture());
    SourceControlEvent sourceControlEvent = sourceControlEventArgumentCaptor.getValue();
    assertThat(sourceControlEvent.getEventType()).isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    assertThat(sourceControlEvent.getApplicationId()).isEqualTo(sourceControl.getOwnerId());
    assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(Stage.ID_SOURCE);
    assertThat(sourceControlEvent.getScanTriggerType())
        .isEqualTo(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
    assertThat(sourceControlEvent.getBranchName()).isEqualTo(sourceControl.getBaseBranch());
  }
}
