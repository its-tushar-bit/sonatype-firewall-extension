/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.git.event.EventTestUtils.createEvent;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class SimultaneousEventSelectionRuleTest
{
  @Test
  public void testCanPushEvent() {
    SimultaneousEventSelectionRule simultaneousEventSelectionRule = new SimultaneousEventSelectionRule(GITHUB);

    // iterate over each event type
    SourceControlEvent.EVENT_TYPES.forEach(targetEventType -> {
      // get the limits for that event type
      SourceControlEvent event = createEvent().setEventType(targetEventType);
      int strictCount = simultaneousEventSelectionRule.getAllowedEventCount(event, true);
      int relaxedCount = simultaneousEventSelectionRule.getAllowedEventCount(event, false);

      Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();
      int sameEvents = 0;

      // no events in progress should always return true
      assertThat(simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, true)).isTrue();
      assertThat(simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, false)).isTrue();

      do {
        // add an event of the target type
        event = createEvent().setEventType(targetEventType);
        eventsInProgress.put(event.getApplicationId(), event);
        sameEvents++;

        // add an event of another type for noise to make sure they don't impact the expected results
        SourceControlEvent anotherEvent = createEvent().setEventType(getDifferentEventTypeThan(targetEventType));
        eventsInProgress.put(anotherEvent.getApplicationId(), anotherEvent);

        assertThat(simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, true))
            .isEqualTo(strictCount < 0 || sameEvents < strictCount);
        assertThat(simultaneousEventSelectionRule.canPushEvent(event, eventsInProgress, false))
            .isEqualTo(relaxedCount < 0 || sameEvents < relaxedCount);
      }
      while (sameEvents <= relaxedCount);
    });
  }

  private String getDifferentEventTypeThan(String eventType) {
    return SourceControlEvent.EVENT_TYPES.stream().filter(type -> !type.equalsIgnoreCase(eventType)).findAny().get();
  }
}
