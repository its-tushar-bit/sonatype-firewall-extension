/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import java.net.UnknownHostException;
import java.util.UUID;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UserEventManagerTest
{
  @Mock
  private SourceControlEventDAO mockSourceControlEventDAO;

  @Mock
  private SourceControlEventProcessor mockSourceControlEventProcessor;

  @Mock
  private SourceControlUtils mockSourceControlUtils;

  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
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
          new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, provider,
              mockSourceControlUtils);

      // when: add the event
      userEventManager.addEvent(event);

      // then: it should mark in progress and push to processing
      verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
      verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
    }
  }

  @Test
  public void testOnEventCompleted_queuedEventPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, GITLAB, mockSourceControlUtils)
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
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventPartiallyCompleted_queuedEventPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, GITHUB, mockSourceControlUtils)
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
    userEventManager.onEventPartiallyCompleted(completedEvent, reason);

    // then: completed event should be marked as such and queued event should be sent for processing
    verify(mockSourceControlEventDAO, times(1)).markEventPartiallyComplete(eq(completedEvent.getId()), eq(reason));
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventError_errorEventNoRetry() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, AZURE, mockSourceControlUtils)
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
    userEventManager.onEventError(errorEvent, new Exception(errorMsg));

    // then: event marked as error and no events processed
    verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(errorEvent.getId()), eq(errorMsg));
    verify(mockSourceControlEventDAO, never()).insert(any());
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventError_errorEventWithRetry() throws InterruptedException {
    // given: event manager
    UserEventManager userEventManager = new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor,
        BITBUCKET, mockSourceControlUtils)
            .setSuspensionTimeoutForTesting(1);

    // capture the retry event we expect to be inserted into the DB
    ArgumentCaptor<SourceControlEvent> retryEventCaptor = ArgumentCaptor.forClass(SourceControlEvent.class);
    doNothing().when(mockSourceControlEventDAO).insert(retryEventCaptor.capture());

    // when: report an event error
    SourceControlEvent errorEvent =
        new SourceControlEvent().forRemediationPullRequest().withId("error-event").setApplicationId("errorApp");
    String errorMsg = "for testing";

    userEventManager.onEventError(errorEvent, new UnknownHostException(errorMsg));

    // the retry errors also have suspension rules associated with them;  so, we need to wait for the suspension to
    // expire and then do something to trigger event processing - such as simulating completion of an event
    Thread.sleep(1_500);
    userEventManager.onEventCompleted(createEvent().setEventType(SourceControlEvent.STATUS_UPDATE_EVENT));

    // then: event marked as error
    verify(mockSourceControlEventDAO, times(1)).markEventHasError(eq(errorEvent.getId()), eq(errorMsg));

    // and then: the retry event was sent for processing
    verify(mockSourceControlEventProcessor, times(1))
        .processEvent(eq(retryEventCaptor.getValue()), eq(userEventManager));
  }
}
