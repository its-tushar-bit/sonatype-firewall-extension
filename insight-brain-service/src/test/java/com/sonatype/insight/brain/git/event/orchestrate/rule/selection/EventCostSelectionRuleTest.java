/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.Test;

import static com.sonatype.insight.brain.git.event.orchestrate.rule.selection.EventCostSelectionRule.MAX_IN_PROGRESS_EVENT_POINTS;
import static org.assertj.core.api.Assertions.assertThat;

public class EventCostSelectionRuleTest
{
  @Test
  public void testEventCostSelectionRule() {
    // given: no events in progress
    List<SourceControlEvent> inProgressEvents = new ArrayList<>();
    EventCostSelectionRule rule = new EventCostSelectionRule();

    // no events in progress should return max points available
    assertThat(rule.getAvailableEventPoints(inProgressEvents)).isEqualTo(MAX_IN_PROGRESS_EVENT_POINTS);

    // check each event type individually
    SourceControlEvent.EVENT_TYPES.forEach(type -> {
      inProgressEvents.clear();
      SourceControlEvent event = new SourceControlEvent().setEventType(type);
      inProgressEvents.add(event);

      // make sure each event can be pushed in isolation
      assertThat(rule.canPushEvent(event, MAX_IN_PROGRESS_EVENT_POINTS)).isTrue();

      // make sure event can be pushed if cost = available
      int eventCost = rule.getEventCost(event);
      assertThat(eventCost).isGreaterThanOrEqualTo(0);
      assertThat(rule.canPushEvent(event, eventCost)).isTrue();

      // make sure event cannot be pushed if cost > available
      assertThat(rule.canPushEvent(event, eventCost - 1)).isFalse();
    });
  }
}
