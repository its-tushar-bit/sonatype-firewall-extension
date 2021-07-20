/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.eq;
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
    // given: an event that should push immediately
    SourceControlEvent event = new SourceControlEvent()
        .forStatusUpdate()
        .withId("event-1")
        .setApplicationId("app-1");

    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlUtils);

    // when: add the event
    userEventManager.addEvent(event);

    // then: it should mark in progress and push to processing
    verify(mockSourceControlEventDAO, times(1)).markEventInProgress(eq(event.getId()));
    verify(mockSourceControlEventProcessor, times(1)).processEvent(eq(event), eq(userEventManager));
  }

  @Test
  public void testOnEventCompleted_queuedEventPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlUtils)
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
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlUtils)
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
  public void testOnEventError_queuedEventNotPushed() {
    // given: event manager with an event queued up
    UserEventManager userEventManager =
        new UserEventManager(mockSourceControlEventDAO, mockSourceControlEventProcessor, mockSourceControlUtils)
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
    verify(mockSourceControlEventDAO, never()).markEventInProgress(eq(event.getId()));
    verify(mockSourceControlEventProcessor, never()).processEvent(eq(event), eq(userEventManager));
  }
}
