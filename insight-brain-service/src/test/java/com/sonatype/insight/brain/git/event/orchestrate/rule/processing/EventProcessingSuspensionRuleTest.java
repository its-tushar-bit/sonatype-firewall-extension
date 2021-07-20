/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import org.junit.Test;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static org.assertj.core.api.Assertions.assertThat;

public class EventProcessingSuspensionRuleTest
{
  @Test
  public void testCanPushEvent_noSuspensions() {
    EventProcessingSuspensionRule eventProcessingSuspensionRule = new EventProcessingSuspensionRule();
    assertThat(eventProcessingSuspensionRule.isEventProcessingSuspended()).isFalse();
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isFalse();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
    });
  }

  @Test
  public void testCanPushEvent_scmNotificationEventSuspension() {
    EventProcessingSuspensionRule eventProcessingSuspensionRule = new EventProcessingSuspensionRule();

    // set an error that causes suspension of scm notification related events
    eventProcessingSuspensionRule.onEventProcessingError(new Exception("blah blah abuse detection blah blah"));
    assertThat(eventProcessingSuspensionRule.isEventProcessingSuspended()).isFalse();
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isTrue();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event))
          .isEqualTo(!eventProcessingSuspensionRule.isScmNotificationEvent(event));
    });
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspendedForUnknownHost() {
    EventProcessingSuspensionRule eventProcessingSuspensionRule = new EventProcessingSuspensionRule();

    // set an error that causes suspension of all events
    eventProcessingSuspensionRule.onEventProcessingError(new UnknownHostException());
    assertThat(eventProcessingSuspensionRule.isEventProcessingSuspended()).isTrue();
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isFalse();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();
    });
  }

  @Test
  public void testCanPushEvent_eventProcessingSuspendedForExclusiveAccessTimeout() {
    EventProcessingSuspensionRule eventProcessingSuspensionRule = new EventProcessingSuspensionRule();

    // set an error that causes suspension of all events
    eventProcessingSuspensionRule.onEventProcessingError(new ExclusiveAccessRequestTimeoutException(""));
    assertThat(eventProcessingSuspensionRule.isEventProcessingSuspended()).isTrue();
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isFalse();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();
    });
  }

  @Test
  public void testIsScmNotificationEvent() {
    EventProcessingSuspensionRule suspensionRule = new EventProcessingSuspensionRule();

    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forApplicationEvaluation())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forDiscoveredPullRequest())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forRepositoryUrlUpdated())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forSourceControlEvaluation())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forStatusUpdate())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forUpdatedPullRequest())).isFalse();

    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forRemediationPullRequest())).isTrue();
  }
}
