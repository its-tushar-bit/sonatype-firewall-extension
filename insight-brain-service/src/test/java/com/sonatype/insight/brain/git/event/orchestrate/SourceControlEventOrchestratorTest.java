/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.git.event.SourceControlEventPublisher;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.service.ScmNodeProcessor;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.GitRepositoryInfo;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import static com.sonatype.nexus.scm.SourceControlProvider.AZURE;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventOrchestratorTest
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private SourceControlEventProcessor mockSourceControlEventProcessor;

  @Mock
  private SourceControlEventPublisher mockSourceControlEventPublisher;

  @Mock
  private SourceControlLoadBalancer mockSourceControlLoadBalancer;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private IqForScmLicenseChecker mockIqForScmLicenseChecker;

  @Mock
  private ApiConfigFeaturesService mockApiConfigFeaturesService;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ScmNodeProcessor scmNodeProcessor;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(mockIqForScmLicenseChecker.isIqForScmSupported()).thenReturn(true);
  }

  @Test
  public void testNewExecutor() {
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    ScheduledExecutorService scheduledExecutorService = sourceControlEventOrchestrator.newExecutor();

    verify(mockShutdownHandler).add(scheduledExecutorService);
  }

  @Test
  public void testOnNewEvent_differentUsersDifferentListeners() {
    // given: an orchestrator and two events for different scm users
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(true);
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("https://gitlab.org/organization/project", null, "user", "token", GITLAB,
            "base-branch", true, true, true, true, true, true, false, null);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    SourceControlEvent user1Event =
        new SourceControlEvent().forRemediationPullRequest()
            .withId("user1Event")
            .setScmUsername("user1")
            .setApplicationId("user1App");
    SourceControlEvent user2Event =
        new SourceControlEvent().forRemediationPullRequest()
            .withId("user2Event")
            .setScmUsername("user2")
            .setApplicationId("user2App");

    // when: process the events and capture the callback references
    sourceControlEventOrchestrator.onNewEvent(user1Event);

    ArgumentCaptor<SourceControlEventStatusListener> user1StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event), user1StatusListenerCaptor.capture());

    sourceControlEventOrchestrator.onNewEvent(user2Event);
    ArgumentCaptor<SourceControlEventStatusListener> user2StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user2Event), user2StatusListenerCaptor.capture());

    // then: the callback references are different, indicating that different user event managers were used
    assertThat(user1StatusListenerCaptor.getValue()).isNotEqualTo(user2StatusListenerCaptor.getValue());
  }

  @Test
  public void testOnNewEvent_sameUsersSameListeners() {
    // given: an orchestrator and two events for the same scm user
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(true);
    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("https://bitbucket.org/organization/project", null, "user", "token", BITBUCKET,
            "base-branch", true, true, true, true, true, true, false, null);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    SourceControlEvent user1Event1 =
        new SourceControlEvent().forRemediationPullRequest()
            .withId("user1Event1")
            .setScmUsername("user1")
            .setApplicationId("user1App1");
    SourceControlEvent user1Event2 =
        new SourceControlEvent().forStatusUpdate()
            .withId("user1Event2")
            .setScmUsername("user1")
            .setApplicationId("user1App2");

    // when: process both events and capture the callback references
    sourceControlEventOrchestrator.onNewEvent(user1Event1);

    ArgumentCaptor<SourceControlEventStatusListener> user1StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event1), user1StatusListenerCaptor.capture());

    sourceControlEventOrchestrator.onNewEvent(user1Event2);
    ArgumentCaptor<SourceControlEventStatusListener> user2StatusListenerCaptor =
        ArgumentCaptor.forClass(SourceControlEventStatusListener.class);
    verify(mockSourceControlEventProcessor).processEvent(eq(user1Event2), user2StatusListenerCaptor.capture());

    // then: the callback references should match indicating that the same user event manager processed both events
    assertThat(user1StatusListenerCaptor.getValue()).isEqualTo(user2StatusListenerCaptor.getValue());
  }

  @Test
  public void testOnNewEvent_eventNotProcessed() {
    // given: the orchestrator can't process the given event
    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(false);

    SourceControlEvent event =
        new SourceControlEvent().forSourceControlEvaluation().setApplicationId(UUID.randomUUID().toString());

    // when: submit event for processing
    sourceControlEventOrchestrator.onNewEvent(event);

    // then: event is not processed
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), any());
  }

  @Test
  public void testFetchAndRouteEvents() throws Exception {
    // given: orchestrator configured to run scheduled executor and fetch events from DB
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(true);
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    GitRepositoryInfo gitRepositoryInfo =
        new GitRepositoryInfo("https://azure.org/organization/project", null, "user", "token", AZURE,
            "base-branch", true, true, true, true, true, true, false, null);
    when(mockSourceControlUtils.getGitRepositoryInfoForApplication(any())).thenReturn(gitRepositoryInfo);

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    SourceControlEvent event =
        new SourceControlEvent().forDiscoveredPullRequest().setApplicationId(UUID.randomUUID().toString());
    List<SourceControlEvent> events = new ArrayList<>();
    events.add(event);
    when(mockSourceControlLoadBalancer.acquireEventsToProcess()).thenReturn(events);

    // when: start the orchestrator which will start the executor service to query the DB for events
    sourceControlEventOrchestrator.setEventProcessingScheduleTimesForTesting(1, 2);

    SourceControlEventOrchestrator spyOrchestrator = Mockito.spy(sourceControlEventOrchestrator);
    CountDownLatch executorFiredLatch = new CountDownLatch(1);
    unlatch(executorFiredLatch).when(spyOrchestrator).notifyRoutingComplete();
    spyOrchestrator.register();

    verifyUnlatched(executorFiredLatch, 10);
    verify(mockSourceControlEventProcessor).processEvent(eq(event), any());
  }

  @Test
  public void testStart_unlicensed() {
    when(mockIqForScmLicenseChecker.isIqForScmSupported()).thenReturn(false);

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    // the orchestrator starts, but it does nothing
    sourceControlEventOrchestrator.register();
    verify(mockSourceControlLoadBalancer, never()).acquireEventsToProcess();
  }

  @Test
  public void testStart_whenFeatureIsNotEnabled() {
    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(false);

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    SourceControlEventOrchestrator spyOrchestrator = Mockito.spy(sourceControlEventOrchestrator);

    // the orchestrator starts, but it does nothing
    spyOrchestrator.register();

    verify(mockSourceControlLoadBalancer, never()).acquireEventsToProcess();
  }

  @Test
  public void testStop() {
    when(scmNodeProcessor.shouldRun()).thenReturn(true);

    when(mockApiConfigFeaturesService.isSaasLifecycleScmEnabled()).thenReturn(true);

    SourceControlEventOrchestrator sourceControlEventOrchestrator = new SourceControlEventOrchestrator(
        mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlEventPublisher,
        mockSourceControlLoadBalancer, mockIqForScmLicenseChecker, mockSourceControlUtils,
        mockApiConfigFeaturesService, mockShutdownHandler,
        scmNodeProcessor);

    SourceControlEventOrchestrator spyOrchestrator = Mockito.spy(sourceControlEventOrchestrator);

    spyOrchestrator.register();
    spyOrchestrator.deregister();

    verify(spyOrchestrator, times(1)).notifyExecutorShutdown();
  }

  private Stubber unlatch(CountDownLatch latch) {
    return doAnswer(a -> {
      latch.countDown();
      return null;
    });
  }

  private void verifyUnlatched(CountDownLatch latch, long seconds) throws InterruptedException {
    assertThat(latch.await(seconds, TimeUnit.SECONDS)).isTrue();
  }
}
