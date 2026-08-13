/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.util.Date;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PerformanceThrottlingRuleTest
{
  @Test
  public void testCanPushEvents_default() {
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();

    assertThat(rule.canPushEvents()).isTrue();
  }

  @Test
  public void testOnEventProcessed_minStatusTimingUnchanged() {
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();

    // current time for create time will result in minimal event duration, which should not cause the status timing
    // to update
    Date now = new Date();
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate().setCreateTime(now).setCompleteTime(now);

    final long statusTimingBefore = rule.getMinCommitStatusTimingMs();
    rule.onEventProcessed(event);
    final long statusTimingAfter = rule.getMinCommitStatusTimingMs();

    assertThat(statusTimingAfter).isEqualTo(statusTimingBefore);

    // not set the time near the upper bound of the acceptable range
    final long secondsToAge = rule.getAcceptableDurationSeconds() - 1;
    ageEventInSeconds(event, secondsToAge);

    final long statusTimingBefore2 = rule.getMinCommitStatusTimingMs();
    rule.onEventProcessed(event);
    final long statusTimingAfter2 = rule.getMinCommitStatusTimingMs();

    assertThat(statusTimingAfter2).isEqualTo(statusTimingBefore2);
  }

  @Test
  public void testOnEventProcessed_minStatusTimingAdjusted() {
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();

    // given: an event with duration less than the current duration value
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate();
    final long secondsToAge = rule.getMinCommitStatusTimingMs() / 1_000 - 1;
    ageEventInSeconds(event, secondsToAge);

    final long statusTimingBefore = rule.getMinCommitStatusTimingMs();
    rule.onEventProcessed(event);
    final long statusTimingAfter = rule.getMinCommitStatusTimingMs();

    // min timing was adjusted down
    assertThat(statusTimingAfter).isLessThan(statusTimingBefore);
  }

  @Test
  public void testOnEventProcessed_performanceDegradedThenSuspensionExpired() throws Exception {
    // given: simulate degraded performance for the status update event
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();
    assertThat(rule.canPushEvents()).isTrue();

    // age the event so it's outside the acceptable range
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate();
    ageEventInSeconds(event, rule.getAcceptableDurationSeconds() + 1);
    rule.onEventProcessed(event);

    assertThat(rule.canPushEvents()).isFalse();

    // wait for the suspension period to expire
    Thread.sleep((PerformanceThrottlingRule.EVENT_SUSPENSION_SECONDS + 1) * 1_000);
    assertThat(rule.canPushEvents()).isTrue();
  }

  @Test
  public void testOnEventProcessed_performanceDegradedThenImproved() {
    // given: simulate degraded performance for the status update event
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();
    assertThat(rule.canPushEvents()).isTrue();

    // age the event so it's outside the acceptable range
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate();
    ageEventInSeconds(event, rule.getAcceptableDurationSeconds() + 1);
    rule.onEventProcessed(event);

    assertThat(rule.canPushEvents()).isFalse();

    // when: another event is processed with an acceptable duration
    event.setCreateTime(new Date()).setCompleteTime(new Date());
    rule.onEventProcessed(event);

    assertThat(rule.canPushEvents()).isTrue();
  }

  @Test
  public void testOnEventProcessed_performanceDegradedByEventType() {
    // given: the rule and an event with an unacceptable duration
    PerformanceThrottlingRule rule = new PerformanceThrottlingRule();
    SourceControlEvent event = new SourceControlEvent();

    SourceControlEvent.EVENT_TYPES.forEach(type -> {
      // reset throttling
      ageEventInSeconds(event, 0);
      event.forStatusUpdate();
      rule.onEventProcessed(event);
      assertThat(rule.canPushEvents()).isTrue();

      // verify all event types can push when duration is acceptable
      event.setEventType(type);
      rule.onEventProcessed(event);
      assertThat(rule.canPushEvents()).isTrue();

      // verify only the status update event affects the throttling behavior
      ageEventInSeconds(event, rule.getAcceptableDurationSeconds() + 1);
      rule.onEventProcessed(event);
      assertThat(rule.canPushEvents()).isEqualTo(!SourceControlEvent.STATUS_UPDATE_EVENT.equals(type));
    });
  }

  private void ageEventInSeconds(SourceControlEvent event, long secondsToAge) {
    Date now = new Date();
    event.setCreateTime(new Date(System.currentTimeMillis() - secondsToAge * 1_000));
    event.setCompleteTime(now);
  }
}
