/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.sonatype.insight.brain.concurrent.LazyInitThreadPoolExecutor;
import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.PullRequestCommentingEventHandler;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.PullRequestStateEventHandler;
import com.sonatype.insight.brain.git.SourceControlScanService;
import com.sonatype.insight.brain.git.SourceControlService;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.brain.common.metering.MeteredThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.nexus.git.utils.api.GitException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventProcessor.DEFAULT_MAX_THREAD_POOL_SIZE;
import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventProcessorTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestCommentingEventHandler mockPullRequestCommentingEventHandler;

  @Mock
  private PullRequestStateEventHandler mockPullRequestStateEventHandler;

  @Mock
  private PullRequestRemediationService mockPullRequestRemediationService;

  @Mock
  private GitCommitStatusService mockGitCommitStatusService;

  @Mock
  private TenantReference<SemaphorePool> poolTenantReference;

  @Mock
  private SemaphorePool mockRepoAccessController;

  @Mock
  private SourceControlScanService mockSourceControlScanService;

  @Mock
  private SourceControlService mockSourceControlService;

  @Mock
  private SourceControlEventStatusListener mockStatusListener;

  @Mock
  private CurrentUser mockCurrentUser;

  @Mock
  private Configuration configuration;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  private SourceControlEventProcessor sourceControlEventProcessor;

  private Level originalLogLevel;

  public SourceControlEventProcessorTest() {
    super(SourceControlEventProcessor.class);
  }

  @BeforeEach
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    when(poolTenantReference.get()).thenReturn(mockRepoAccessController);
    when(mockCurrentUser.getUsernameOrSystem()).thenReturn(CurrentUser.SYSTEM);
    when(configuration.getSourceControlEventProcessorPoolSize()).thenReturn(DEFAULT_MAX_THREAD_POOL_SIZE);
    sourceControlEventProcessor = spy(new SourceControlEventProcessor(
        mockPullRequestCommentingEventHandler,
        mockPullRequestStateEventHandler,
        mockPullRequestRemediationService,
        mockGitCommitStatusService,
        mockSourceControlScanService,
        mockSourceControlService,
        mockCurrentUser,
        configuration,
        mockShutdownHandler));
    sourceControlEventProcessor.setRepoAccessController(poolTenantReference);

    // Set logger to DEBUG level to ensure all log messages are captured
    // This prevents flakiness when previous tests may have changed the logger level
    Logger log =
        (Logger) org.slf4j.LoggerFactory.getLogger(SourceControlEventProcessor.class);
    originalLogLevel = log.getLevel();
    log.setLevel(Level.DEBUG);
  }

  @AfterEach
  public void teardown() {
    // Restore original logger level to avoid affecting other tests
    Logger log =
        (Logger) org.slf4j.LoggerFactory.getLogger(SourceControlEventProcessor.class);
    log.setLevel(originalLogLevel);
  }

  @Test
  public void testSourceControlEventProcessor_AddsExecutorToShutdownHandler() {
    LazyInitThreadPoolExecutor lazyInitThreadPoolExecutor = sourceControlEventProcessor.getLazyInitThreadPoolExecutor();
    ThreadPoolExecutor threadPoolExecutor = lazyInitThreadPoolExecutor.getThreadPoolExecutor();

    verify(mockShutdownHandler).add(threadPoolExecutor, ShutdownPriority.SOURCE_CONTROL_EVENT_PROCESSOR);
  }

  @Test
  public void testProcessEvent_applicationEvaluation() throws InterruptedException {
    SourceControlEvent event = createEvent().forApplicationEvaluation();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onApplicationEvaluation(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_discoveredPullRequest() throws InterruptedException {
    SourceControlEvent event = createEvent().forDiscoveredPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onDiscoveredPullRequest(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_remediationPullRequest() throws IOException, InterruptedException {
    SourceControlEvent event = createEvent().forRemediationPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestRemediationService, times(1)).onRemediateComponent(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_repositoryUrlUpdated() throws InterruptedException {
    SourceControlEvent event = createEvent().forRepositoryUrlUpdated();

    processEventAndWaitForCompletion(event);

    verify(mockSourceControlService, times(1)).onRepositoryUrlUpdated(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_sourceControlEvaluation() throws GitException, IOException, InterruptedException {
    SourceControlEvent event = createEvent().forSourceControlEvaluation();

    processEventAndWaitForCompletion(event);

    verify(mockSourceControlScanService, times(1)).onSourceControlScan(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_statusUpdate() throws InterruptedException {
    SourceControlEvent event = createEvent().forStatusUpdate();

    processEventAndWaitForCompletion(event);

    verify(mockGitCommitStatusService, times(1)).onSendCommitStatus(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_updatedPullRequest() throws InterruptedException {
    SourceControlEvent event = createEvent().forUpdatedPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, times(1)).onUpdatedPullRequest(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_runAsNonSystemUser() throws Exception {
    when(mockCurrentUser.getUsernameOrSystem()).thenReturn("JohnDoe");
    SourceControlEvent event = createEvent().forUpdatedPullRequest();

    processEventAndWaitForCompletion(event);

    verify(mockPullRequestCommentingEventHandler, never()).onUpdatedPullRequest(any());
    verifyEventNotStarted(event);
    verifyEventError(event,
        "SourceControlEvent with ID " + event.getId() + " processed as user 'JohnDoe' instead of 'system'");
  }

  @Test
  public void testProcessEvents_interruptOnAcquireRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("event-1");

    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).acquire(eq(event.getApplicationId()));

    // when:
    processEventAndWaitForCompletion(event);

    // then:
    verifyEventStarted(event);
    assertThatLogMessagesContain(
        debug("Unable to acquire repo access for application 'app1'"));
  }

  @Test
  public void testProcessEvents_interruptOnReleaseRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("hij789");

    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).release(eq(event.getApplicationId()));

    // when:
    processEventAndWaitForCompletion(event);

    // then:
    verifyEventStarted(event);
    verifyEventCompleted(event);
    assertThatLogMessagesEqual(
        debug(getProcessedEventMessage(event)),
        warn("Unable to release repo access for application 'app1'"));
  }

  @Test
  public void testProcessEvent_prStateUpdate() throws InterruptedException {
    // given: a PR_STATE_UPDATE_EVENT event
    SourceControlEvent event = createEvent();
    event.setEventType(SourceControlEvent.PR_STATE_UPDATE_EVENT);

    // when: the event is processed
    processEventAndWaitForCompletion(event);

    // then: the event is delegated to the handler
    verify(mockPullRequestStateEventHandler, times(1)).handle(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_batchPrStateUpdate() throws InterruptedException {
    // given: a BATCH_PR_STATE_UPDATE_EVENT event
    SourceControlEvent event = createEvent();
    event.setEventType(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);

    // when: the event is processed
    processEventAndWaitForCompletion(event);

    // then: the event is delegated to the handler
    verify(mockPullRequestStateEventHandler, times(1)).handle(eq(event));
    verifyEventStarted(event);
    verifyEventCompleted(event);
  }

  @Test
  public void testProcessEvent_prStateUpdate_handlingError() throws InterruptedException {
    // given: a PR_STATE_UPDATE_EVENT event and an error in the handler
    SourceControlEvent event = createEvent();
    event.setEventType(SourceControlEvent.PR_STATE_UPDATE_EVENT);
    doThrow(new RuntimeException("Test error")).when(mockPullRequestStateEventHandler).handle(eq(event));

    // when: the event is processed
    processEventAndWaitForCompletion(event);

    // then: the error is reported through the status listener
    verify(mockPullRequestStateEventHandler, times(1)).handle(eq(event));
    verifyEventStarted(event);
    verifyEventError(event, "Test error");
  }

  @Test
  public void testProcessEvent_batchPrStateUpdate_handlingError() throws InterruptedException {
    // given: a BATCH_PR_STATE_UPDATE_EVENT event and an error in the handler
    SourceControlEvent event = createEvent();
    event.setEventType(SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT);
    doThrow(new RuntimeException("Test error")).when(mockPullRequestStateEventHandler).handle(eq(event));

    // when: the event is processed
    processEventAndWaitForCompletion(event);

    // then: the error is reported as a partial failure
    verify(mockPullRequestStateEventHandler, times(1)).handle(eq(event));
    verifyEventStarted(event);
    verifyEventError(event, "Test error");
  }

  @Test
  public void testProcessEvent_StatusUpdateProcessesWithoutLocking() throws InterruptedException {
    // Given: a STATUS_UPDATE_EVENT
    SourceControlEvent statusUpdateEvent = createEvent();
    statusUpdateEvent.setEventType(SourceControlEvent.STATUS_UPDATE_EVENT);

    // When: the event is processed
    processEventAndWaitForCompletion(statusUpdateEvent);

    // Then: the event should be processed successfully without acquiring any locks
    verify(mockRepoAccessController, never()).acquire(any(String.class));
    verify(mockRepoAccessController, never()).release(any(String.class));
    verify(mockGitCommitStatusService, times(1)).onSendCommitStatus(eq(statusUpdateEvent));
    verifyEventStarted(statusUpdateEvent);
    verifyEventCompleted(statusUpdateEvent);
  }

  @Test
  public void testProcessEvent_ApplicationEvaluation_ApplicationLevelLockingPreserved() throws InterruptedException {
    // Given: an APPLICATION_EVALUATION_EVENT
    SourceControlEvent appEvalEvent = createEvent();
    appEvalEvent.setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);

    // When: the event is processed
    processEventAndWaitForCompletion(appEvalEvent);

    // Then: application-level locking should still be used
    verify(mockRepoAccessController, times(1)).acquire(eq(appEvalEvent.getApplicationId()));
    verify(mockRepoAccessController, times(1)).release(eq(appEvalEvent.getApplicationId()));
    verify(mockPullRequestCommentingEventHandler, times(1)).onApplicationEvaluation(eq(appEvalEvent));
    verifyEventStarted(appEvalEvent);
    verifyEventCompleted(appEvalEvent);
  }

  @Test
  public void testProcessEvent_StatusUpdateDoesNotBlockOtherEvents() throws InterruptedException, GitException, IOException {
    // Given: STATUS_UPDATE_EVENT and SOURCE_CONTROL_EVALUATION_EVENT for the same application
    SourceControlEvent statusUpdateEvent = createEvent();
    statusUpdateEvent.setEventType(SourceControlEvent.STATUS_UPDATE_EVENT);

    SourceControlEvent repoScanEvent = createEvent();
    repoScanEvent.setApplicationId(statusUpdateEvent.getApplicationId()); // Same application ID
    repoScanEvent.setEventType(SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT);

    CountDownLatch statusUpdateLatch = createOnEventFinishedLatch(statusUpdateEvent);
    CountDownLatch repoScanLatch = createOnEventFinishedLatch(repoScanEvent);

    // When: both events are processed in parallel
    sourceControlEventProcessor.processEvent(statusUpdateEvent, mockStatusListener);
    sourceControlEventProcessor.processEvent(repoScanEvent, mockStatusListener);

    // Wait for both events to complete
    verifyUnlatched(statusUpdateLatch);
    verifyUnlatched(repoScanLatch);

    // Then: Only ONE acquire call should happen (from SOURCE_CONTROL_EVALUATION_EVENT)
    // STATUS_UPDATE_EVENT should never acquire locks, allowing it to run in parallel
    verify(mockRepoAccessController, times(1)).acquire(eq(repoScanEvent.getApplicationId()));
    verify(mockRepoAccessController, times(1)).release(eq(repoScanEvent.getApplicationId()));

    // And both events should complete successfully
    verify(mockGitCommitStatusService, times(1)).onSendCommitStatus(eq(statusUpdateEvent));
    verify(mockSourceControlScanService, times(1)).onSourceControlScan(eq(repoScanEvent));
  }

  @Test
  public void testProcessEvent_Meters() throws Exception {
    Field field = MeteredThreadPoolExecutor.class.getDeclaredField("injectedMeterRegistry");
    try {
      MeterRegistry meterRegistry = new SimpleMeterRegistry();
      field.setAccessible(true);
      field.set(null, meterRegistry);

      Map<String, String> expectedThreadPoolTags = Map.of(
          "kind", "source_control_events",
          "name", "SourceControlEventProcessor");

      for (String eventType : SourceControlEvent.EVENT_TYPES) {
        SourceControlEvent event = createEvent();
        event.setEventType(eventType);

        processEventAndWaitForCompletion(event);

        verifyEventStarted(event);
        verifyEventCompleted(event);

        Map<String, String> expectedRunnableTags = Map.of(
            "kind", "source_control_events",
            "name", "SourceControlEventProcessor",
            "source_control_event_type", eventType.replaceAll(" ", "_"));
        Assertions.assertThat(meterRegistry.getMeters())
            .extracting(meter -> Tuple.tuple(
                meter.getId().getName(),
                meter.getId()
                    .getTags()
                    .stream()
                    .collect(Collectors.toMap(Tag::getKey, Tag::getValue))))
            .contains(
                Tuple.tuple("executor.active", expectedThreadPoolTags),
                Tuple.tuple("executor.queued", expectedThreadPoolTags),
                Tuple.tuple("executor.queue.remaining", expectedThreadPoolTags),
                Tuple.tuple("executor.pool.size", expectedThreadPoolTags),
                Tuple.tuple("executor.pool.core", expectedThreadPoolTags),
                Tuple.tuple("executor.pool.max", expectedThreadPoolTags),
                Tuple.tuple("executor.idle", expectedRunnableTags),
                Tuple.tuple("executor", expectedRunnableTags),
                Tuple.tuple("executor.failed", expectedRunnableTags),
                Tuple.tuple("executor.completed", expectedRunnableTags));
      }
    }
    finally {
      field.set(null, null);
      field.setAccessible(false);
    }
  }

  private CountDownLatch createOnEventFinishedLatch(SourceControlEvent event) {
    CountDownLatch latch = new CountDownLatch(1);
    unlatch(latch).when(sourceControlEventProcessor).notifyFinishedProcessingEvent(eq(event));
    return latch;
  }

  private void processEventAndWaitForCompletion(SourceControlEvent event) throws InterruptedException {
    CountDownLatch eventProcessedLatch = createOnEventFinishedLatch(event);
    sourceControlEventProcessor.processEvent(event, mockStatusListener);
    verifyUnlatched(eventProcessedLatch);
  }

  private Stubber unlatch(CountDownLatch latch) {
    return doAnswer(a -> {
      latch.countDown();
      return null;
    });
  }

  private void verifyEventCompleted(SourceControlEvent event) {
    verify(mockStatusListener, times(1)).onEventCompleted(eq(event));
  }

  private void verifyEventStarted(SourceControlEvent event) {
    verify(mockStatusListener, times(1)).onEventStarted(eq(event));
  }

  private void verifyEventNotStarted(SourceControlEvent event) {
    verify(mockStatusListener, never()).onEventStarted(eq(event));
  }

  private void verifyEventError(SourceControlEvent event, String errorMessage) {
    ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
    verify(mockStatusListener, times(1)).onEventError(eq(event), exceptionCaptor.capture());
    assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(errorMessage);
  }

  private void verifyUnlatched(CountDownLatch latch) throws InterruptedException {
    verifyUnlatched(latch, 5);
  }

  private void verifyUnlatched(CountDownLatch latch, long seconds) throws InterruptedException {
    assertThat(latch.await(seconds, TimeUnit.SECONDS)).isTrue();
  }

  private String getProcessedEventMessage(SourceControlEvent event) {
    return format("Processed event '%s' of type '%s' for application '%s'", event.getId(), event.getEventType(),
        event.getApplicationId());
  }
}
