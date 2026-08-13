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

import static org.assertj.core.api.Assertions.assertThat;

public class SingleApplicationSelectionRuleTest
{
  @Test
  public void testCanPushEvent_sameApplicationId() {
    // given: an event in progress and a new event for the same app
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate().setApplicationId("app1");
    Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();
    eventsInProgress.put(event.getApplicationId(), event);
    SourceControlEvent event2 = new SourceControlEvent().forApplicationEvaluation().setApplicationId("app1");

    // when: see if we can push a new event for same app
    assertThat(new SingleApplicationSelectionRule().canPushEvent(event2, eventsInProgress)).isFalse();
  }

  @Test
  public void testCanPushEvent_differentApplicationId() {
    // given: an event in progress and a new event for a different app
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate().setApplicationId("app1");
    Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();
    eventsInProgress.put(event.getApplicationId(), event);
    SourceControlEvent event2 = new SourceControlEvent().forApplicationEvaluation().setApplicationId("app2");

    // when: see if we can push a new event for different app
    assertThat(new SingleApplicationSelectionRule().canPushEvent(event2, eventsInProgress)).isTrue();
  }

  @Test
  public void testCanPushEvent_noEventsInProgress() {
    // given: no events in progress
    Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();
    SourceControlEvent event = new SourceControlEvent().forApplicationEvaluation().setApplicationId("app2");

    // when: see if we can push an event
    assertThat(new SingleApplicationSelectionRule().canPushEvent(event, eventsInProgress)).isTrue();
  }

  @Test
  public void testCanPushEvent_missingEvent() {
    // given: an event in progress
    SourceControlEvent event = new SourceControlEvent().forStatusUpdate().setApplicationId("app1");
    Map<String, SourceControlEvent> eventsInProgress = new HashMap<>();
    eventsInProgress.put(event.getApplicationId(), event);

    // when: see if we can push a new event for same app
    assertThat(new SingleApplicationSelectionRule().canPushEvent(null, eventsInProgress)).isFalse();
  }
}
