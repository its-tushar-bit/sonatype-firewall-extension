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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
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
import static org.mockito.Mockito.times;
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
  public void testExecute_DefaultBranchMonitoringFeatureDisabled() {
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);

    DefaultBranchMonitor defaultBranchMonitorSpy = spy(defaultBranchMonitor);
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forUser("username")) {
      defaultBranchMonitorSpy.execute(mock(JobExecutionContext.class));
    }

    verify(defaultBranchMonitorSpy, never()).updateDefaultBranchScans();
  }

  @Test
  public void testExecute_SaasLifecycleSCMFeatureDisabled() {
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

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

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(24),
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
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);
    defaultBranchMonitor.register();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(defaultBranchMonitor);
  }

  @Test
  public void testStart_SaasLifecycleSCMFeatureDisabled() {
    when(mockApiConfigFeaturesService.isDefaultBranchMonitoringEnabled()).thenReturn(false);
    defaultBranchMonitor.register();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(defaultBranchMonitor);
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
  public void testUpdateDefaultBranchScans_doesNotExitOnFirstError() throws Exception {
    // given: 2 applications with outdated scans
    // Service started to initialize interval
    Application app1 = tempEntity.newApplicationWithParent();
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusMinutes(defaultBranchMonitor.getIntervalInMinutes() + 60));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null,
        SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app1.getId(), "http://a.com/org/repo1", null);
    tempEntity.newPolicyEvaluation(app1.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    Application app2 = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(app2.getId(), "http://a.com/org/repo2", null);
    tempEntity.newPolicyEvaluation(app2.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash456", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    AtomicBoolean firstTime = new AtomicBoolean(true);
    doAnswer(invocation -> {
      if (firstTime.get()) {
        firstTime.set(false);
        throw new IllegalArgumentException("simulated exception"); // make it fail on first call
      }
      else {
        return null;
      }
    }).when(sourceControlEventPublisherMock).publishEvent(any());

    // when: detecting and updating default branch with 2 outdated source scans
    defaultBranchMonitor.updateDefaultBranchScans();

    // then: 2 source control event publishing calls are attempted, although the first attempt fails
    verify(sourceControlEventPublisherMock, times(2)).publishEvent(any());
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

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(24),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(new SourceControlConfiguration()));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringStartTime() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTime(LocalTime.of(1, 11));
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(24),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_UpdatedDefaultBranchMonitoringIntervalHours() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringIntervalHours(12);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock).schedulePeriodicTask(defaultBranchMonitor,
        Duration.ofHours(12),
        defaultBranchMonitor.getDefaultBranchMonitorStartTime(sourceControlConfiguration));
  }

  @Test
  public void testSourceControlConfigurationChanged_NoRelevantUpdate() {
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setGitImplementation(GitImplementation.JAVA);
    sourceControlConfigurationDAO.set(sourceControlConfiguration);

    configuration.sourceControlConfigurationChanged();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any());
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
