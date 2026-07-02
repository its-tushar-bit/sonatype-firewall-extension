/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.PullRequestFailureCategory;
import com.sonatype.insight.brain.git.SourceControlException;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static com.sonatype.nexus.scm.SourceControlProvider.AZURE;
import static com.sonatype.nexus.scm.SourceControlProvider.BITBUCKET;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserEventManagerTest
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private SourceControlEventProcessor mockSourceControlEventProcessor;

  @Mock
  private SourceControlLoadBalancer mockSourceControlLoadBalancer;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(mockSourceControlLoadBalancer.reserveEvent(any())).thenReturn(true);
  }

  @Test
  public void testNewExecutor() {
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer,
            mockSourceControlEventProcessor, GITLAB, mockSourceControlUtils, mockShutdownHandler);

    ScheduledExecutorService scheduledExecutorService = userEventManager.newExecutor();

    verify(mockShutdownHandler).add(scheduledExecutorService);
  }

  @Test
  public void testAddEvent_eventPushed() {
    for (SourceControlProvider provider : SourceControlProvider.values()) {
      // given: an event that should push immediately
      SourceControlEvent event = new SourceControlEvent()
          .forStatusUpdate()
          .withId(UUID.randomUUID().toString())
          .setApplicationId("app-1");

      UserEventManager userEventManager =
          new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer,
              mockSourceControlEventProcessor, provider, mockSourceControlUtils, mockShutdownHandler);

      // when: add the event
      userEventManager.addEvent(event);

      // then: it should mark in progress and push to processing
      verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
    }
  }

  @Test
  public void testOnEventCompleted_relatedEventsReleased() {
    // given: event manager with an event queued up and the load balancer unable to reserve the event
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer, mockSourceControlEventProcessor,
            GITLAB, mockSourceControlUtils, mockShutdownHandler)
                .setEventsSuspendedForTesting(true);

    SourceControlEvent event = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("event-1")
        .setApplicationId("app-1");

    when(mockSourceControlLoadBalancer.reserveEvent(eq(event))).thenReturn(false);

    userEventManager.addEvent(event);
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    userEventManager.setEventsSuspendedForTesting(false);

    // when: report an event completed
    SourceControlEvent completedEvent = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("completed")
        .setApplicationId("app-completed");

    userEventManager.onEventCompleted(completedEvent);

    // then: completed event should be marked as such, queued event should NOT be sent for processing, and
    // load balancer should be called to release related events
    verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(completedEvent.getId()));
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    verify(mockSourceControlLoadBalancer, times(1)).releaseRelatedEvents(eq(event), eq(true));
  }

  @Test
  public void testOnEventCompleted_queuedEventPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer, mockSourceControlEventProcessor,
            GITLAB, mockSourceControlUtils, mockShutdownHandler)
                .setEventsSuspendedForTesting(true);

    SourceControlEvent event = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("event-1")
        .setApplicationId("app-1");

    userEventManager.addEvent(event);
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    userEventManager.setEventsSuspendedForTesting(false);

    // when: report an event completed
    SourceControlEvent completedEvent = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("completed")
        .setApplicationId("app-completed");

    userEventManager.onEventCompleted(completedEvent);

    // then: completed event should be marked as such and queued event should be sent for processing
    verify(mockSourceControlEventDAO, times(1)).markEventComplete(eq(completedEvent.getId()));
    verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventPartiallyCompleted_queuedEventPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer, mockSourceControlEventProcessor,
            GITHUB, mockSourceControlUtils, mockShutdownHandler)
                .setEventsSuspendedForTesting(true);

    SourceControlEvent event = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("event-1")
        .setApplicationId("app-1");

    userEventManager.addEvent(event);
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    userEventManager.setEventsSuspendedForTesting(false);

    // when: report an event partially completed
    SourceControlEvent completedEvent = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("completed")
        .setApplicationId("app-completed");

    String reason = "for testing";
    Exception error = new Exception(reason);
    userEventManager.onEventPartiallyCompleted(completedEvent, reason, error);

    // then: completed event should be marked as such and queued event should be sent for processing
    verify(mockSourceControlEventDAO, times(1)).markEventPartiallyComplete(eq(completedEvent.getId()), eq(reason),
        eq(error));
    verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventStarted_queuedEventNotPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer, mockSourceControlEventProcessor,
            GITLAB, mockSourceControlUtils, mockShutdownHandler)
                .setEventsSuspendedForTesting(true);

    SourceControlEvent event = new SourceControlEvent()
        .forDiscoveredPullRequest()
        .withId("event-123")
        .setApplicationId("app-123");

    userEventManager.addEvent(event);
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    userEventManager.setEventsSuspendedForTesting(false);

    // when: report an event started
    SourceControlEvent startedEvent = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("started")
        .setApplicationId("app-started");

    userEventManager.onEventStarted(startedEvent);

    // then: started event should be marked as such and queued event should NOT be sent for processing
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(startedEvent.getId()));
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventError_errorEventNoRetry() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer, mockSourceControlEventProcessor,
            AZURE, mockSourceControlUtils, mockShutdownHandler)
                .setEventsSuspendedForTesting(true);

    SourceControlEvent event = new SourceControlEvent()
        .forUpdatedPullRequest()
        .withId("event-1")
        .setApplicationId("app-1");

    userEventManager.addEvent(event);
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
    userEventManager.setEventsSuspendedForTesting(false);

    // when: report an event error
    SourceControlEvent errorEvent =
        new SourceControlEvent().forRemediationPullRequest().withId("error-event").setApplicationId("errorApp");
    String errorMsg = "for testing";
    Exception error = new Exception(errorMsg);
    userEventManager.onEventError(errorEvent, error);

    // then: event marked as error and no events processed
    verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(errorEvent.getId()), eq(errorMsg), eq(error));
    verify(mockSourceControlEventDAO, never()).insert(any());
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventError_errorEventWithRetry() throws InterruptedException {
    // given: event manager
    UserEventManager userEventManager = new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer,
        mockSourceControlEventProcessor, BITBUCKET, mockSourceControlUtils, mockShutdownHandler)
            .setSuspensionTimeoutForTesting(1);

    // capture the retry event we expect to be inserted into the DB
    ArgumentCaptor<SourceControlEvent> retryEventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    doReturn(1).when(mockSourceControlEventDAO).insert(retryEventCaptor.capture());

    // when: report an event error
    SourceControlEvent errorEvent =
        new SourceControlEvent().forRemediationPullRequest().withId("error-event").setApplicationId("errorApp");
    String errorMsg = "for testing";
    Exception error = new UnknownHostException(errorMsg);

    userEventManager.onEventError(errorEvent, error);

    // the retry errors also have suspension rules associated with them; so, we need to wait for the suspension to
    // expire and then do something to trigger event processing - such as simulating completion of an event
    Thread.sleep(1_500);
    userEventManager.onEventCompleted(createEvent().setEventType(SourceControlEvent.STATUS_UPDATE_EVENT));

    // then: event marked as error
    verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(errorEvent.getId()), eq(errorMsg), eq(error));

    // and then: the retry event was sent for processing
    verify(mockSourceControlEventProcessor, times(1))
        .processEvent(eq(retryEventCaptor.getValue()), eq(userEventManager));
  }

  @Test
  public void testOnEventError_nonRetryableSourceControlException_skipsAutoRetry() {
    // given: event manager
    UserEventManager userEventManager = new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer,
        mockSourceControlEventProcessor, BITBUCKET, mockSourceControlUtils, mockShutdownHandler);

    // when: report a SourceControlException carrying MANIFEST_COMPONENT_NOT_FOUND (non-retryable)
    SourceControlEvent errorEvent =
        new SourceControlEvent().forRemediationPullRequest().withId("error-event").setApplicationId("errorApp");
    SourceControlException error = new SourceControlException(
        "Pull request creation failed: ...", PullRequestFailureCategory.MANIFEST_COMPONENT_NOT_FOUND);

    userEventManager.onEventError(errorEvent, error);

    // then: event marked as error AND no retry event was inserted (DB write of a fresh event is the
    // only side-effect that retryEvent() has, so verifying never() on insert proves no retry happened)
    verify(mockSourceControlEventDAO, times(1))
        .markEventHasError(eq(errorEvent.getId()), eq(error.getMessage()), eq(error));
    verify(mockSourceControlEventDAO, never()).insert(any());
    verify(mockSourceControlEventProcessor, never()).processEvent(any(), eq(userEventManager));
  }

  @Test
  public void testOnEventError_retryableSourceControlException_stillUsesRetryRule() {
    // given: event manager
    UserEventManager userEventManager = new UserEventManager(mockSourceControlEventDAO, mockSourceControlLoadBalancer,
        mockSourceControlEventProcessor, BITBUCKET, mockSourceControlUtils, mockShutdownHandler);

    // when: report a SourceControlException carrying SCM_ERROR (retryable category) — but with a
    // message that EventProcessingErrorRetryRule#isRetryableException does NOT match. The new
    // gate must not short-circuit; the rule decides.
    SourceControlEvent errorEvent =
        new SourceControlEvent().forRemediationPullRequest().withId("error-event").setApplicationId("errorApp");
    SourceControlException error = new SourceControlException(
        "Failed to execute pull request for application 'errorApp'", PullRequestFailureCategory.SCM_ERROR);

    userEventManager.onEventError(errorEvent, error);

    // then: event marked as error; the retry rule's exception-type filter does not match this
    // generic message either, so still no retry — but the gate did not short-circuit.
    verify(mockSourceControlEventDAO, times(1))
        .markEventHasError(eq(errorEvent.getId()), eq(error.getMessage()), eq(error));
    verify(mockSourceControlEventDAO, never()).insert(any());
  }
}
