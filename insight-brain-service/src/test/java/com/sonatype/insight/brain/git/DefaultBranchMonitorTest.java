/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableMap;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class DefaultBranchMonitorTest
    extends AbstractComponentTest
{
  @Inject
  private InsightConfig insightConfig;

  @Inject
  private DefaultBranchMonitor defaultBranchMonitor;

  @Mock
  private TaskScheduler taskSchedulerMock;

  @Mock
  private SourceControlEventPublisher sourceControlEventPublisherMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(taskSchedulerMock);
    binder.bind(SourceControlEventPublisher.class).toInstance(sourceControlEventPublisherMock);
    super.configure(binder);
  }

  @Test
  public void testDisallowConcurrentExecution() {
    assertThat(JobBuilder.newJob(DefaultBranchMonitor.class).build().isConcurrentExectionDisallowed()).isTrue();
  }

  @Test
  public void testExecute() {
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
  public void testStart_FeatureEnabled() throws Exception {
    Date expectedStartTime = defaultBranchMonitor.getDefaultBranchMonitorStartTime();
    defaultBranchMonitor.start();

    verify(taskSchedulerMock).schedulePeriodicTask(DefaultBranchMonitor.class, DefaultBranchMonitor.TASK_NAME,
        Duration.ofHours(24),
        expectedStartTime);
  }

  @Test
  public void testStart_FeatureDisabled() throws Exception {
    insightConfig.setFeatures(ImmutableMap.of(Feature.DEFAULT_BRANCH_MONITORING.getFlag(), false));
    defaultBranchMonitor.start();

    verify(taskSchedulerMock, never()).schedulePeriodicTask(any(), any(), any());
    verify(taskSchedulerMock).unscheduleTask(DefaultBranchMonitor.TASK_NAME);
  }

  @Test
  public void testUpdatePullRequestDetails_applicationToUpdateScan() throws Exception {
    // given: application with outdated scan
    // Service started to initialize interval
    defaultBranchMonitor.start();
    Application app = tempEntity.newApplicationWithParent();
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(defaultBranchMonitor.getIntervalInHours() + 1));
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
  public void testUpdatePullRequestDetails_noApplicationsToScan() throws Exception {
    // given: application without scan
    // Service started to initialize interval
    defaultBranchMonitor.start();
    Application app = tempEntity.newApplicationWithParent();
    LocalDateTime now = LocalDateTime.now();
    Date scanTime = toDate(now.minusHours(defaultBranchMonitor.getIntervalInHours() - 1));
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITLAB);
    tempEntity.newSourceControl(app.getId(), "http://a.com/org/repo", null);
    tempEntity.newPolicyEvaluation(app.getId(), StageTypes.SOURCE.getId(), "scanId", false, false, false, scanTime,
        "commitHash123", ScanTriggerType.SOURCE_CONTROL_INTERNAL_ONBOARDING);

    // when: detecting and updating default branch with outdated source scans
    defaultBranchMonitor.updateDefaultBranchScans();

    // then: no source control event is sent
    verify(sourceControlEventPublisherMock, never()).publishEvent(any());
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
