/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import org.apache.http.client.HttpResponseException;
import org.junit.Test;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static com.sonatype.insight.brain.git.event.EventTestUtils.createEventForApp;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationScopeEventProcessingSuspensionRuleTest
{
  @Test
  public void testCanPushEvent_eventProcessingSuspended_UnknownHostException() {
    assertEventProcessingSuspended(new RuntimeException(new UnknownHostException()));
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspended_SocketTimeoutException() {
    assertEventProcessingSuspended(new RuntimeException(new SocketTimeoutException()));
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspended_HttpResponseException_BadGateway() {
    assertEventProcessingSuspended(new RuntimeException(new HttpResponseException(0, "foo Bad Gateway bar")));
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspended_ExclusiveAccessRequestTimeoutException() {
    assertEventProcessingSuspended(new RuntimeException(new ExclusiveAccessRequestTimeoutException("")));
  }

  private void assertEventProcessingSuspended(Exception exception) {
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule();

    SourceControlEvent event = createEvent();

    // set an error that causes suspension of all events for a given application
    eventProcessingSuspensionRule.onEventProcessingError(event, exception);
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();

    // verify any event for same app cannot be pushed
    SourceControlEvent eventSameApp = createEventForApp(event.getApplicationId());
    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      eventSameApp.setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(eventSameApp)).isFalse();
    });

    // verify any event for a different app can still be pushed
    SourceControlEvent eventDifferentApp = createEvent();
    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      eventDifferentApp.setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(eventDifferentApp)).isTrue();
    });
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspended_HttpResponseException_NotBadGateway() {
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule();

    SourceControlEvent event = createEvent();

    // HttpResponseException other than "Bad Gateway" should not suspend event processing
    eventProcessingSuspensionRule.onEventProcessingError(event,
        new RuntimeException(new HttpResponseException(0, "test")));
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
  }

  @Test
  public void testCanPushEvent_nonSuspendableException() {
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule();

    SourceControlEvent event = createEvent();
    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      event.setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
    });
  }

  @Test
  public void testCanPushEvent_suspensionExpires() throws InterruptedException {
    // given: the suspension rule in a suspended state for an application (with shortened suspension times for testing)
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule().setTimeoutsForTesting(1);

    SourceControlEvent event = createEvent();

    // verify unknown host timeout
    eventProcessingSuspensionRule.onEventProcessingError(event, new RuntimeException(new UnknownHostException()));
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();

    Thread.sleep(1_100);
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();

    // verify exclusive access timeout
    eventProcessingSuspensionRule.onEventProcessingError(event,
        new RuntimeException(new ExclusiveAccessRequestTimeoutException("")));
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();

    Thread.sleep(1_100);
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
  }

  @Test
  public void testOnEventProcessed_suspensionCleared() {
    // given: the suspension rule in a suspended state for an application
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule();

    SourceControlEvent event = createEvent();
    eventProcessingSuspensionRule.onEventProcessingError(event, new UnknownHostException());
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();

    // when: an event is successfully processed for the same app
    eventProcessingSuspensionRule.onEventProcessed(createEventForApp(event.getApplicationId()));

    // then: suspension state is cleared
    assertThat(eventProcessingSuspensionRule.canPushEvent(createEventForApp(event.getApplicationId())))
        .isTrue();
  }

  @Test
  public void testOnEventProcessed_suspensionNotCleared() {
    // given: the suspension rule in a suspended state for two applications
    ApplicationScopeEventProcessingSuspensionRule eventProcessingSuspensionRule =
        new ApplicationScopeEventProcessingSuspensionRule();

    SourceControlEvent app1Event = createEvent();
    eventProcessingSuspensionRule.onEventProcessingError(app1Event, new UnknownHostException());
    assertThat(eventProcessingSuspensionRule.canPushEvent(app1Event)).isFalse();

    SourceControlEvent app2Event = createEvent();
    eventProcessingSuspensionRule.onEventProcessingError(app2Event, new UnknownHostException());
    assertThat(eventProcessingSuspensionRule.canPushEvent(app2Event)).isFalse();

    // when: an event is successfully processed for one of the apps
    eventProcessingSuspensionRule.onEventProcessed(createEventForApp(app1Event.getApplicationId()));

    // then: suspension state is cleared for that app, but not the other
    assertThat(eventProcessingSuspensionRule.canPushEvent(createEventForApp(app1Event.getApplicationId())))
        .isTrue();
    assertThat(eventProcessingSuspensionRule.canPushEvent(createEventForApp(app2Event.getApplicationId())))
        .isFalse();
  }
}
