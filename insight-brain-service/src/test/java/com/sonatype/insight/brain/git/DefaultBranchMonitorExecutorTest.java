/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlConfiguration;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultBranchMonitorExecutorTest
{
  private DefaultBranchMonitorExecutor underTest;

  @Mock
  private Configuration mockConfiguration;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Mock
  private SourceControlDAO mockSourceControlDAO;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Before
  public void setUp() throws Exception {
    underTest = new DefaultBranchMonitorExecutor(mockConfiguration, mockTaskScheduler,
        mockSourceControlDAO, mockSourceControlEventPublisher);
  }

  @Test
  public void testStart_FeatureEnabled() {
    when(mockConfiguration.getSourceControlConfigurationOrDefault()).thenReturn(new SourceControlConfiguration());

    Date expectedStartTime = underTest.getDefaultBranchMonitorStartTime(new SourceControlConfiguration());
    InsightJob job = mock(InsightJob.class);
    underTest.schedule(job);

    verify(mockTaskScheduler).schedulePeriodicTask(job,
        Duration.ofHours(24),
        expectedStartTime);
  }

  @Test
  public void testGetDefaultBranchMonitorStartTime_NotNull_DoesNotPlusRandomMinutes() {
    DefaultBranchMonitorExecutor spy = spy(underTest);
    lenient().when(spy.getRandomizedStartOffsetInMinutes()).thenReturn(5);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString("00:00");

    assertThat(spy.getDefaultBranchMonitorStartTime(sourceControlConfiguration)).hasMinute(0);
  }

  @Test
  public void testGetDefaultBranchMonitorStartTime_Null_UsesDefaultPlusRandomMinutes() {
    DefaultBranchMonitorExecutor spy = spy(underTest);
    lenient().when(spy.getRandomizedStartOffsetInMinutes()).thenReturn(5);
    SourceControlConfiguration sourceControlConfiguration = new SourceControlConfiguration();
    sourceControlConfiguration.setDefaultBranchMonitoringStartTimeString(null);

    assertThat(spy.getDefaultBranchMonitorStartTime(sourceControlConfiguration)).hasMinute(5);
  }

  @Test
  public void testUpdateDefaultBranchScans_applicationToUpdateScan() throws Exception {
    // given: application with outdated scan
    // Service started to initialize interval
    SourceControl sc = new SourceControl.Builder()
        .setOwnerId("ownerId")
        .setBaseBranch("main")
        .setProvider(SourceControlProvider.GITLAB)
        .build();

    when(mockSourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(any()))
        .thenReturn(Collections.singletonList(sc));

    // when: detecting and updating default branch with outdated source scans
    underTest.performScan(mock(InsightJob.class));

    // then: source control event is published with proper source control information
    verifySourceControlEventWasSent(sc);
  }

  @Test
  public void testUpdateDefaultBranchScans_doesNotExitOnFirstError() throws Exception {
    // given: 2 applications with outdated scans
    SourceControl sc1 = new SourceControl.Builder()
        .setOwnerId("ownerId1")
        .setBaseBranch("main1")
        .setProvider(SourceControlProvider.GITLAB)
        .build();

    SourceControl sc2 = new SourceControl.Builder()
        .setOwnerId("ownerId2")
        .setBaseBranch("main2")
        .setProvider(SourceControlProvider.GITHUB)
        .build();

    when(mockSourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(any()))
        .thenReturn(Arrays.asList(sc1, sc2));

    doNothing().doThrow(new IllegalArgumentException("simulated exception"))
        .when(mockSourceControlEventPublisher)
        .publishEvent(any());

    // when: detecting and updating default branch with 2 outdated source scans
    underTest.performScan(any());

    // then: 2 source control event publishing calls are attempted, although the first attempt fails
    verify(mockSourceControlEventPublisher, times(2)).publishEvent(any());
  }

  @Test
  public void testUpdateDefaultBranchScans_noApplicationsToScan() throws Exception {
    // given: application without scan
    when(mockSourceControlDAO.getCompositeSourceControlForOutdatedSourceScans(any()))
        .thenReturn(Collections.emptyList());

    // when: detecting and updating default branch with outdated source scans
    underTest.performScan(any());

    // then: no source control event is sent
    verify(mockSourceControlEventPublisher, never()).publishEvent(any());
  }

  private void verifySourceControlEventWasSent(SourceControl sourceControl) {
    ArgumentCaptor<SourceControlEvent> sourceControlEventArgumentCaptor =
        ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventPublisher).publishEvent(sourceControlEventArgumentCaptor.capture());
    SourceControlEvent sourceControlEvent = sourceControlEventArgumentCaptor.getValue();
    Assertions.assertThat(sourceControlEvent.getEventType())
        .isEqualTo(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);
    Assertions.assertThat(sourceControlEvent.getApplicationId()).isEqualTo(sourceControl.getOwnerId());
    Assertions.assertThat(sourceControlEvent.getStageTypeId()).isEqualTo(Stage.ID_SOURCE);
    Assertions.assertThat(sourceControlEvent.getScanTriggerType())
        .isEqualTo(ScanTriggerType.SOURCE_CONTROL_INTERNAL_DEFAULT_BRANCH_MONITORING);
    Assertions.assertThat(sourceControlEvent.getBranchName()).isEqualTo(sourceControl.getBaseBranch());
  }
}
