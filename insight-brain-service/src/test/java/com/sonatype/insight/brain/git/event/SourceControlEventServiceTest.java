/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.GitCommitStatusService;
import com.sonatype.insight.brain.git.ManifestScanService;
import com.sonatype.insight.brain.git.PullRequestCommentingService;
import com.sonatype.insight.brain.git.PullRequestRemediationService;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Stubber;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SourceControlEventServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private PullRequestCommentingService mockPullRequestCommentingService;

  @Mock
  private PullRequestRemediationService mockPullRequestRemediationService;

  @Mock
  private GitCommitStatusService mockGitCommitStatusService;

  @Mock
  private ManifestScanService mockManifestScanService;

  @Mock
  private SemaphorePool mockRepoAccessController;

  private SourceControlEventService eventService;

  public SourceControlEventServiceTest() {
    super(SourceControlEventService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
    eventService = spy(new SourceControlEventService(mockSourceControlEventDAO, mockPullRequestCommentingService,
        mockPullRequestRemediationService, mockGitCommitStatusService, mockManifestScanService));
  }

  @After
  public void tearDown() {
    if (null != eventService) {
      CountDownLatch shutdownLatch = createOnShutdownCompleteLatch(100);
      eventService.shutdown();
      try {
        verifyUnlatched(shutdownLatch);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Test
  public void testProcessEvents_onApplicationEvaluationEvent() throws Exception {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(events.get(0));

    // when: process the events
    eventService.processEvents();

    // then: pull request processing invoked for the given event
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onAppEval,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_onDiscoveredPullRequestEvent() throws Exception {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(events.get(0));

    // when: process the events
    eventService.processEvents();

    // then: pull request processing invoked for the given event
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onPrDiscovered,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_onRemediationPullRequestEvent() throws Exception {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(events.get(0));

    // when: process the events
    eventService.processEvents();

    // then: remediation processing invoked for the given event
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onComponentRemediation,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_onStatusUpdateEvent() throws Exception {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.STATUS_UPDATE_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(events.get(0));

    // when: process the events
    eventService.processEvents();

    // then: status update invoked for the given event
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onStatusUpdate,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_onManifestScanEvent() throws Exception {
    // given: an event DAO setup to return a manifest scan event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.MANIFEST_SCAN_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(events.get(0));

    // when: process the events
    eventService.processEvents();

    // then: manifest scan invoked for the given event
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onManifestScan,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_onErrorHandlingEvent() throws Exception {
    // given: a list of events and the pull request commenting service configured to throw an exception on a certain
    //        event
    List<SourceControlEvent> events = generateEvents(
        "1:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT,
        "1:app2:" + SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT,
        "1:app2:" + SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    final String errorMsg = "simulated";
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);
    doThrow(new RuntimeException(errorMsg)).when(mockPullRequestCommentingService)
        .onDiscoveredPullRequest(any(SourceControlEvent.class));

    CountDownLatch event0Latch = createOnEventFinishedLatch(events.get(0));
    CountDownLatch event1Latch = createOnEventFinishedLatch(events.get(1));
    CountDownLatch event2Latch = createOnEventFinishedLatch(events.get(2));

    // when:
    eventService.processEvents();

    // then: all events complete even though only one resulted in an error
    verifyUnlatched(event0Latch);
    verifyUnlatched(event1Latch);
    verifyUnlatched(event2Latch);

    verifyProcessEventsActions(events.get(0),
        EventProcessAction.markedInProgress,
        EventProcessAction.onAppEval,
        EventProcessAction.markedComplete);

    verifyProcessEventsActions(events.get(1), errorMsg,
        EventProcessAction.markedInProgress,
        EventProcessAction.onPrDiscovered,
        EventProcessAction.markedHasError);

    verifyProcessEventsActions(events.get(2),
        EventProcessAction.markedInProgress,
        EventProcessAction.onComponentRemediation,
        EventProcessAction.markedComplete);

    assertThatLogMessagesContain(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 3"),
        debug(getProcessedEventMessage(events.get(0))),
        error(getProcessedEventErrorMessage(events.get(1), errorMsg)),
        debug(getProcessedEventMessage(events.get(2)))
    );
  }

  @Test
  public void testProcessEvents_eventCounts() {
    // given: a list of events
    List<SourceControlEvent> events = generateEvents("12:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);

    // and given: an event DAO setup to return the list of events and the count of events requested
    ArgumentCaptor<Integer> requestCountCaptor = ArgumentCaptor.forClass(Integer.class);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), requestCountCaptor.capture()))
        .thenReturn(events);

    // and given: commenting service setup to block on requests to cause the event pool to back up
    final Lock lock = new ReentrantLock();
    doAnswer(a -> {
      lock.lock();
      return null;
    }).when(mockPullRequestCommentingService).onApplicationEvaluation(any(SourceControlEvent.class));

    // when: process events for an "unloaded" event service, which will load up the service
    eventService.processEvents();

    // then: the maximum number of events was requested
    verify(mockSourceControlEventDAO, atLeast(1))
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), requestCountCaptor.capture());
    assertThat(requestCountCaptor.getValue()).isEqualTo(SourceControlEventService.TASK_QUEUE_CAPACITY);

    // and when: another request to process events is made (while n - 1 of the previous events are being processed)
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThat(eventService.getNumberOfEventsToRequest())
          .isEqualTo(SourceControlEventService.TASK_QUEUE_CAPACITY - 1);
    });

    eventService.processEvents();

    // then: the count of events requested = max load - those still in work from previous request
    final int expectedRequestCount = SourceControlEventService.MAX_LOAD - (events.size() - 1);
    verify(mockSourceControlEventDAO, atLeast(2))
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), requestCountCaptor.capture());
    assertThat(requestCountCaptor.getValue()).isEqualTo(expectedRequestCount);

    // and then: logs are correct based on whichever event was actually picked up for processing first
    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(eventService, times(1)).notifyFinishedProcessingEvent(eventCaptor.capture());
    SourceControlEvent completedEvent = eventCaptor.getValue();

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 12"),
        debug(getProcessedEventMessage(completedEvent)),
        debug("Requested " + (SourceControlEventService.TASK_QUEUE_CAPACITY - 1) +
            " source control events, processing 12")
    );
  }

  @Test
  public void testProcessEvents_multipleInvocationsQuickSuccession() throws InterruptedException {
    // given : given a process events invocation that will take a little time to complete
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO.selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch startupLatch = new CountDownLatch(1);
    unlatchWithDelay(startupLatch, 500).when(mockSourceControlEventDAO).markEventInProgress(events.get(0).getId());

    CountDownLatch finishLatch = createOnEventFinishedLatch(events.get(0));

    new Thread(() -> eventService.processEvents()).start();

    // make sure the thread inside the event service starts up first
    verifyUnlatched(startupLatch, 3);

    // when: invoke process events a second time while first one is running
    eventService.processEvents();

    // then: 2nd invocation should do nothing and log so
    verifyUnlatched(finishLatch);
    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug("skipping event processing this cycle as previous cycle is still running"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_eventOverload() throws Exception {
    // given: DAO setup to return more events than the event service is expecting
    List<SourceControlEvent> events = generateEvents(
        2 * SourceControlEventService.TASK_QUEUE_CAPACITY + ":app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    // and given: commenting service setup to block on requests to cause the event pool to back up
    final Lock lock = new ReentrantLock();
    doAnswer(a -> {
      lock.lock();
      return null;
    }).when(mockPullRequestCommentingService).onApplicationEvaluation(any(SourceControlEvent.class));

    // when:
    int eventsProcessed = eventService.processEvents();

    // then: we processed at least the 'desired load' number of events; absence of an error here indicates that we did
    //       not put more work in progress than the pool is configured to handle while at the same time ingesting as
    //       much as we could;  note: the first event runs to completion but never releases the lock above so subsequent
    //       events must wait (per test design);  thus, the process count can be greater than the desired load as some
    //       of the work can complete while the events are being consumed, thus allowing us to process more events
    assertThat(eventsProcessed).isGreaterThanOrEqualTo(SourceControlEventService.TASK_QUEUE_CAPACITY);

    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing "
              + 2 * SourceControlEventService.TASK_QUEUE_CAPACITY),
          debug(getProcessedEventMessage(events.get(0)))
      );
    });

    // special cleanup for this test to keep its log messages from bleeding over into other tests
    CountDownLatch shutdownLatch = createOnShutdownCompleteLatch(3000);
    eventService.shutdown();
    eventService = null;
    verifyUnlatched(shutdownLatch);
  }

  @Test
  /*
    The purpose of this test is to ensure there are no concurrency issues when processing a burst of event messages
    for the same application;  load testing revealed a race condition that allowed multiple events for the same
    application to execute in parallel;  a symptom of this was the WARN messages that appeared in the logs when repo
    access was released multiple times for same application
   */
  public void testProcessEvents_burstLoadSameApplication() throws Exception {
    // given: DAO setup to return max number of events that can be processed in one cycle
    final int eventCount = SourceControlEventService.TASK_QUEUE_CAPACITY;
    List<SourceControlEvent> events =
        generateEvents(eventCount + ":app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    CountDownLatch eventBurstCompleteLatch = createEventBurstCompleteLatch(eventCount);

    // when: process the events
    int eventsProcessed = eventService.processEvents();

    // then: all events processed and no warnings or errors in the logs
    assertThat(eventsProcessed).isEqualTo(eventCount);
    verifyUnlatched(eventBurstCompleteLatch);
    assertNoWarningsInLogs();
    assertNoErrorsInLogs();
  }

  @Test
  public void testProcessEvents_invalidEventType() throws Exception {
    // given: DAO setup to return an event with an unsupported type
    final String eventId = "event-id-1";
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .withId(eventId)
        .setEventType("SomeUnknownEventType");
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(event);

    // when:
    eventService.processEvents();

    // then:
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(event, "invalid event type",
        EventProcessAction.markedInProgress,
        EventProcessAction.markedHasError,
        EventProcessAction.noPropagation);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        warn("Invalid source control event type 'SomeUnknownEventType' for event 'event-id-1'")
    );
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventInProgress() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("c0c0babe");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    doThrow(new RuntimeException("simulated")).when(mockSourceControlEventDAO).markEventInProgress(eq(event.getId()));

    // when:
    eventService.processEvents();

    // then:
    verifyProcessEventsActions(event, EventProcessAction.markedInProgress, EventProcessAction.noPropagation);
    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        error("Error marking event in progress for event 'c0c0babe' of type 'application evaluation' for" +
            " application 'app1' : simulated")
    );
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventComplete() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .withId("abc123")
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    final String errorMsg = "simulated error";
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    doThrow(new RuntimeException(errorMsg)).when(mockSourceControlEventDAO).markEventComplete(eq(event.getId()));

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(event);

    // when:
    eventService.processEvents();

    // then:
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(event, errorMsg,
        EventProcessAction.markedInProgress,
        EventProcessAction.onAppEval,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(event)),
        error("Error updating event processing status for event 'abc123' of type 'application evaluation' for" +
            " application 'app1' : " + errorMsg)
    );
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventHasError() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("def456");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    doThrow(new RuntimeException("simulated")).when(mockPullRequestCommentingService)
        .onApplicationEvaluation(eq(event));
    doThrow(new RuntimeException("simulated")).when(mockSourceControlEventDAO)
        .markEventHasError(eq(event.getId()), any());

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(event);

    // when:
    eventService.processEvents();

    // then:
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(event, "simulated",
        EventProcessAction.markedInProgress,
        EventProcessAction.onAppEval,
        EventProcessAction.markedHasError);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        error("Unable to process event 'def456' of type 'application evaluation' for application 'app1' : simulated"),
        error("Error updating event processing status for event 'def456' of type 'application evaluation' for"
            + " application 'app1' : simulated")
    );
  }

  @Test
  public void testProcessEvents_interruptOnAcquireRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("c0c0babe");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    eventService.setRepoAccessController(mockRepoAccessController);
    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).acquire(eq(event.getApplicationId()));

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(event);

    // when:
    eventService.processEvents();

    // then:
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(event,
        EventProcessAction.markedInProgress,
        EventProcessAction.noPropagation);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug("Unable to acquire repo access for application 'app1'")
    );
  }

  @Test
  public void testProcessEvents_interruptOnReleaseRepoAccessControl() throws Exception {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("hij789");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    eventService.setRepoAccessController(mockRepoAccessController);
    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).release(eq(event.getApplicationId()));

    CountDownLatch eventsProcessedLatch = createOnEventFinishedLatch(event);

    // when:
    eventService.processEvents();

    // then:
    verifyUnlatched(eventsProcessedLatch);
    verifyProcessEventsActions(event,
        EventProcessAction.markedInProgress,
        EventProcessAction.onAppEval,
        EventProcessAction.markedComplete);

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug(getProcessedEventMessage(event)),
        error("Unable to release repo access for application 'app1'")
    );
  }

  @Test
  public void testInitializeEvents() {
    // when: call to initialize events
    eventService.initializeEvents();

    // then: dao is called to clear existing event reservations
    verify(mockSourceControlEventDAO, times(1)).clearEventReservations();
  }

  private String getProcessedEventMessage(SourceControlEvent event) {
    return format("Processed event '%s' of type '%s' for application '%s'", event.getId(), event.getEventType(),
        event.getApplicationId());
  }

  private String getProcessedEventErrorMessage(SourceControlEvent event, String errorMessage) {
    return format("Unable to process event '%s' of type '%s' for application '%s' : %s", event.getId(),
        event.getEventType(), event.getApplicationId(), errorMessage);
  }

  private List<SourceControlEvent> generateEvents(String... qtyAppIdAndTypeArray) {
    List<SourceControlEvent> events = new ArrayList<>();

    for (String qtyAppIdAndType : qtyAppIdAndTypeArray) {
      String[] parts = qtyAppIdAndType.split("[:]");
      int quantity = Integer.parseInt(parts[0]);
      for (int i = 0; i < quantity; i++) {
        SourceControlEvent event = new SourceControlEvent()
            .setApplicationId(parts[1])
            .setEventType(parts[2]);
        event.setId(UUID.randomUUID().toString());
        events.add(event);
      }
    }

    return events;
  }

  /*
    Helpers for synchronizing the test code with the multi-threaded code being tested
   */

  private CountDownLatch createEventBurstCompleteLatch(int burstCount) {
    CountDownLatch latch = new CountDownLatch(burstCount);
    unlatch(latch).when(eventService).notifyFinishedProcessingEvent(any(SourceControlEvent.class));
    return latch;
  }

  private CountDownLatch createOnEventFinishedLatch(SourceControlEvent event) {
    CountDownLatch latch = new CountDownLatch(1);
    unlatch(latch).when(eventService).notifyFinishedProcessingEvent(eq(event));
    return latch;
  }

  private CountDownLatch createOnShutdownCompleteLatch(long delayMilliseconds) {
    CountDownLatch latch = new CountDownLatch(1);
    unlatchWithDelay(latch, delayMilliseconds).when(eventService).notifyShutdownComplete();
    return latch;
  }

  private Stubber unlatch(CountDownLatch latch) {
    return doAnswer(a -> {
      latch.countDown();
      return null;
    });
  }

  private Stubber unlatchWithDelay(CountDownLatch latch, long delayMilliseconds) {
    return doAnswer(a -> {
      latch.countDown();
      sleep(delayMilliseconds);
      return null;
    });
  }

  private void verifyUnlatched(CountDownLatch latch) throws InterruptedException {
    verifyUnlatched(latch, 5);
  }

  private void verifyUnlatched(CountDownLatch latch, long seconds) throws InterruptedException {
    assertThat(latch.await(seconds, TimeUnit.SECONDS)).isTrue();
  }

  /*
    Helpers for verifying the expected outcomes of processing events
   */

  private enum EventProcessAction
  {
    noPropagation, markedInProgress, markedComplete, markedHasError, onAppEval, onPrDiscovered, onComponentRemediation,
    onManifestScan, onStatusUpdate
  }

  private void verifyProcessEventsActions(SourceControlEvent event, EventProcessAction... conditions)
      throws Exception
  {
    verifyProcessEventsActions(event, "no message specified", conditions);
  }

  private void verifyProcessEventsActions(
      SourceControlEvent event,
      String message,
      EventProcessAction... actions)
      throws Exception
  {
    Set<EventProcessAction> actionSet = Sets.newHashSet(actions);

    if (actionSet.contains(EventProcessAction.markedInProgress)) {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    }
    else {
      verify(mockSourceControlEventDAO, never()).markEventInProgress(eq(event.getId()));
    }

    if (actionSet.contains(EventProcessAction.markedComplete)) {
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(event.getId()));
    }
    else {
      verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
    }

    if (actionSet.contains(EventProcessAction.markedHasError)) {
      verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(event.getId()), eq(message));
    }
    else {
      verify(mockSourceControlEventDAO, never()).markEventHasError(eq(event.getId()), any());
    }

    if (actionSet.contains(EventProcessAction.noPropagation)) {
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
      verify(mockPullRequestRemediationService, never()).onRemediateComponent(eq(event));
      verify(mockManifestScanService, never()).onManifestScan(any(SourceControlEvent.class));
      verify(mockGitCommitStatusService, never()).onSendCommitStatus(eq(event));
    }
    else if (actionSet.contains(EventProcessAction.onAppEval)) {
      verify(mockPullRequestCommentingService, times(1)).onApplicationEvaluation(eq(event));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestRemediationService, never()).onRemediateComponent(eq(event));
      verify(mockManifestScanService, never()).onManifestScan(any(SourceControlEvent.class));
      verify(mockGitCommitStatusService, never()).onSendCommitStatus(eq(event));
    }
    else if (actionSet.contains(EventProcessAction.onPrDiscovered)) {
      verify(mockPullRequestCommentingService, times(1)).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
      verify(mockPullRequestRemediationService, never()).onRemediateComponent(eq(event));
      verify(mockManifestScanService, never()).onManifestScan(any(SourceControlEvent.class));
      verify(mockGitCommitStatusService, never()).onSendCommitStatus(eq(event));
    }
    else if (actionSet.contains(EventProcessAction.onComponentRemediation)) {
      verify(mockPullRequestRemediationService, times(1)).onRemediateComponent(eq(event));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
      verify(mockManifestScanService, never()).onManifestScan(any(SourceControlEvent.class));
      verify(mockGitCommitStatusService, never()).onSendCommitStatus(eq(event));
    }
    else if (actionSet.contains(EventProcessAction.onManifestScan)) {
      verify(mockManifestScanService, times(1)).onManifestScan(eq(event));
      verifyNoMoreInteractions(mockPullRequestCommentingService, mockPullRequestRemediationService,
          mockGitCommitStatusService);
    }
    else if (actionSet.contains(EventProcessAction.onStatusUpdate)) {
      verify(mockGitCommitStatusService, times(1)).onSendCommitStatus(eq(event));
      verifyNoMoreInteractions(mockPullRequestCommentingService, mockPullRequestRemediationService,
          mockManifestScanService);
    }
  }
}
