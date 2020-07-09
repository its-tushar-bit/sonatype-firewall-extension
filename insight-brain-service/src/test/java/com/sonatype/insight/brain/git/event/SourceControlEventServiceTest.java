/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sonatype.insight.brain.concurrent.SemaphorePool;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.PullRequestCommentingService;
import com.sonatype.insight.brain.git.VerifiableLoggingTestBase;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SourceControlEventServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private PullRequestCommentingService mockPullRequestCommentingService;

  @Mock
  private SemaphorePool mockRepoAccessController;

  private SourceControlEventService eventService;

  public SourceControlEventServiceTest() {
    super(SourceControlEventService.class);
  }

  @Before
  @Override
  public void setup() {
    try {
      sleep(500);
    }
    catch (InterruptedException e) {
      currentThread().interrupt();
    }
    MockitoAnnotations.initMocks(this);
    super.setup();
    eventService = new SourceControlEventService(mockSourceControlEventDAO, mockPullRequestCommentingService);
  }

  @After
  public void tearDown() {
    if (null != eventService) {
      eventService.shutdown();
    }
  }

  @Test
  public void testProcessEvents_onApplicationEvaluationEvent() {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    // when: process the events and give a little time for the first event to be worked
    eventService.processEvents();

    // then: pull request processing invoked for the given event
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).reserveEventsForInstance(eq(SourceControlEventService.INSTANCE_ID),
          anyInt());
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(events.get(0).getId()));
      verify(mockPullRequestCommentingService, times(1)).onApplicationEvaluation(eq(events.get(0)));
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(events.get(0).getId()));

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          debug(getProcessedEventMessage(events.get(0)))
      );
    });
  }

  @Test
  public void testProcessEvents_onDiscoveredPullRequestEvent() {
    // given: an event DAO setup to return an application evaluation event
    List<SourceControlEvent> events = generateEvents("1:app1:" + SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);

    // when: process the events and give a little time for the first event to be worked
    eventService.processEvents();

    // then: pull request processing invoked for the given event
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).reserveEventsForInstance(eq(SourceControlEventService.INSTANCE_ID),
          anyInt());
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(events.get(0).getId()));
      verify(mockPullRequestCommentingService, times(1)).onDiscoveredPullRequest(eq(events.get(0)));
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(events.get(0).getId()));

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          debug(getProcessedEventMessage(events.get(0)))
      );
    });
  }

  @Test
  public void testProcessEvents_onErrorHandlingEvent() {
    // given: a list of events and the pull request commenting service configured to throw an exception on a certain
    //        event
    List<SourceControlEvent> events = generateEvents(
        "1:app1:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT,
        "1:app2:" + SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT,
        "1:app2:" + SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(events);
    doThrow(new RuntimeException("simulated")).when(mockPullRequestCommentingService)
        .onDiscoveredPullRequest(any(SourceControlEvent.class));

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(events.get(0).getId()));
      verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(events.get(1).getId()), any());
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(events.get(2).getId()));

      assertThatLogMessagesContain(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 3"),
          debug(getProcessedEventMessage(events.get(0))),
          error(getProcessedEventErrorMessage(events.get(1), "simulated")),
          debug(getProcessedEventMessage(events.get(2)))
      );
    });
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

    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 12"),
        debug(getProcessedEventMessage(events.get(0))),
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
    CountDownLatch finishLatch = new CountDownLatch(1);

    doAnswer(a -> {
      startupLatch.countDown();
      sleep(500);
      return null;
    }).when(mockSourceControlEventDAO).markEventInProgress(events.get(0).getId());

    doAnswer(a -> {
      finishLatch.countDown();
      return null;
    }).when(mockSourceControlEventDAO).markEventComplete(events.get(0).getId());

    new Thread(() -> eventService.processEvents()).start();

    // make sure the thread inside the event service starts up first
    assertThat(startupLatch.await(3, TimeUnit.SECONDS)).isTrue();

    // when: invoke process events a second time while first one is running
    eventService.processEvents();

    // then: 2nd invocation should do nothing an log so
    assertThat(finishLatch.await(5, TimeUnit.SECONDS)).isTrue();
    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug("skipping event processing this cycle as previous cycle is still running"),
        debug(getProcessedEventMessage(events.get(0)))
    );
  }

  @Test
  public void testProcessEvents_eventOverload() {
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
  }

  @Test
  public void testProcessEvents_invalidEventType() {
    // given: DAO setup to return an event with an invalid type
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType("invalid");
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
      verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
      verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(event.getId()), any());

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          warn("Invalid source control event type 'invalid'")
      );
    });
  }

  @Test
  public void testProcessEvents_unsupportedEventType() {
    // given: DAO setup to return an event with an unsupported type
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);
    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
      verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
      verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(event.getId()), eq("unsupported"));

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          warn("Unsupported source control event type 'remediation pull request'")
      );
    });
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventInProgress() {
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
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
    verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
    verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
    verify(mockSourceControlEventDAO, never()).markEventHasError(eq(event.getId()), any());
    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        error("Error marking event in progress for event 'c0c0babe' of type 'application evaluation' for" +
            " application 'app1' : simulated")
    );
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventComplete() {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("c0c0babe");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    doThrow(new RuntimeException("simulated")).when(mockSourceControlEventDAO).markEventComplete(eq(event.getId()));

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, times(1)).onApplicationEvaluation(eq(event));
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(event.getId()));
      verify(mockSourceControlEventDAO, never()).markEventHasError(eq(event.getId()), any());

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          debug(getProcessedEventMessage(event)),
          error("Error updating event processing status for event 'c0c0babe' of type 'application evaluation' for" +
              " application 'app1' : simulated")
      );
    });
  }

  @Test
  public void testProcessEvents_exceptionMarkingEventHasError() {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("c0c0babe");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    doThrow(new RuntimeException("simulated")).when(mockPullRequestCommentingService)
        .onApplicationEvaluation(eq(event));
    doThrow(new RuntimeException("simulated")).when(mockSourceControlEventDAO)
        .markEventHasError(eq(event.getId()), any());

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, times(1)).onApplicationEvaluation(eq(event));
      verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
      verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(event.getId()), any());

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          error(
              "Unable to process event 'c0c0babe' of type 'application evaluation' for application 'app1' : simulated"),
          error("Error updating event processing status for event 'c0c0babe' of type 'application evaluation' for"
              + " application 'app1' : simulated")
      );
    });
  }

  @Test
  public void testProcessEvents_interruptOnAcquireRepoAccessControl() throws InterruptedException {
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

    // when:
    eventService.processEvents();

    // then:
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
    verify(mockPullRequestCommentingService, never()).onApplicationEvaluation(eq(event));
    verify(mockSourceControlEventDAO, never()).markEventComplete(eq(event.getId()));
    verify(mockSourceControlEventDAO, never()).markEventHasError(eq(event.getId()), any());
    assertThatLogMessagesEqual(
        debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
        debug("Unable to acquire repo access for application 'app1'")
    );
  }

  @Test
  public void testProcessEvents_interruptOnReleaseRepoAccessControl() throws InterruptedException {
    // given: DAO setup to throw an exception
    SourceControlEvent event = new SourceControlEvent()
        .setApplicationId("app1")
        .setEventType(SourceControlEvent.APPLICATION_EVALUATION_EVENT);
    event.setId("c0c0babe");

    when(mockSourceControlEventDAO
        .selectEventsForInstance(eq(SourceControlEventService.INSTANCE_ID), anyInt()))
        .thenReturn(ImmutableList.of(event));
    eventService.setRepoAccessController(mockRepoAccessController);
    doThrow(new InterruptedException("simulated")).when(mockRepoAccessController).release(eq(event.getApplicationId()));

    // when:
    eventService.processEvents();

    // then:
    await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockPullRequestCommentingService, never()).onDiscoveredPullRequest(eq(event));
      verify(mockPullRequestCommentingService, times(1)).onApplicationEvaluation(eq(event));
      verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(event.getId()));
      verify(mockSourceControlEventDAO, never()).markEventHasError(eq(event.getId()), any());

      assertThatLogMessagesEqual(
          debug("Requested " + SourceControlEventService.TASK_QUEUE_CAPACITY + " source control events, processing 1"),
          debug(getProcessedEventMessage(event)),
          error("Unable to release repo access for application 'app1'")
      );
    });
  }

  @Test
  public void testPublishEvent() {
    // when: publish a null event
    eventService.publishEvent(null);

    // then: nothing saved to DB
    verify(mockSourceControlEventDAO, never()).insert(any());

    // and when: publish an event
    SourceControlEvent event = new SourceControlEvent().setApplicationId("c0c0babe");
    eventService.publishEvent(event);

    // then: DAO tries to save event
    ArgumentCaptor<SourceControlEvent> eventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    verify(mockSourceControlEventDAO, times(1)).insert(eventCaptor.capture());
    SourceControlEvent persistedEvent = eventCaptor.getValue();
    assertThat(persistedEvent.getApplicationId()).isEqualTo("c0c0babe");
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
}
