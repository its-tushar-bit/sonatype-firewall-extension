/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static org.assertj.core.api.Assertions.assertThat;

public class UserScopeEventProcessingSuspensionRuleTest
{
  @Test
  public void testCanPushEvent_noSuspensions() {
    UserScopeEventProcessingSuspensionRule eventProcessingSuspensionRule = new UserScopeEventProcessingSuspensionRule();
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isFalse();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
    });
  }

  @Test
  public void testCanPushEvent_scmNotificationEventSuspension() {
    UserScopeEventProcessingSuspensionRule eventProcessingSuspensionRule = new UserScopeEventProcessingSuspensionRule();

    // set an error that causes suspension of scm notification related events
    eventProcessingSuspensionRule
        .onEventProcessingError(new SourceControlEvent(), new Exception("blah blah abuse detection blah blah"));
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isTrue();

    SourceControlEvent.EVENT_TYPES.forEach(eventType -> {
      SourceControlEvent event = createEvent().setEventType(eventType);
      assertThat(eventProcessingSuspensionRule.canPushEvent(event))
          .isEqualTo(!eventProcessingSuspensionRule.isScmNotificationEvent(event));
    });
  }

  @Test
  public void testIsScmNotificationEvent() {
    UserScopeEventProcessingSuspensionRule suspensionRule = new UserScopeEventProcessingSuspensionRule();

    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forApplicationEvaluation())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forDiscoveredPullRequest())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forRepositoryUrlUpdated())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forSourceControlEvaluation())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forStatusUpdate())).isFalse();
    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forUpdatedPullRequest())).isFalse();

    assertThat(suspensionRule.isScmNotificationEvent(createEvent().forRemediationPullRequest())).isTrue();
  }

  @Test
  public void testOnEventProcessingError_suspensionExpires() throws InterruptedException {
    UserScopeEventProcessingSuspensionRule eventProcessingSuspensionRule = new UserScopeEventProcessingSuspensionRule()
        .setDefaultSuspensionTimeForTesting(1);
    SourceControlEvent event = createEvent().setEventType(SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT);

    // set an error that causes suspension of scm notification related events
    eventProcessingSuspensionRule.onEventProcessingError(new SourceControlEvent(),
        new Exception("blah blah abuse detection blah blah"));
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isTrue();
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isFalse();

    // wait for suspension to expire and verify it does
    Thread.sleep(1_100);
    assertThat(eventProcessingSuspensionRule.areScmNotificationEventsSuspended()).isFalse();
    assertThat(eventProcessingSuspensionRule.canPushEvent(event)).isTrue();
  }
}
