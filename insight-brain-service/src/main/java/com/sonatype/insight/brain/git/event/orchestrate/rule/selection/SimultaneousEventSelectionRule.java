/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.Map;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.APPLICATION_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.STATUS_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;

public class SimultaneousEventSelectionRule
{
  private static final Logger log = LoggerFactory.getLogger(SimultaneousEventSelectionRule.class);

  /**
   * defines the number of events of the given type that can be processed simultaneously;  this is an arbitrary
   * number but is based on the cost for the given event type;  in other words, we want fewer higher cost events
   * to run simultaneously
   */
  private static final ImmutableMap<String, Integer> SIMULTANEOUS_EVENTS_ALLOWED =
      ImmutableMap.<String, Integer>builder()
          .put(APPLICATION_EVALUATION_EVENT, 2)
          .put(DISCOVERED_PULL_REQUEST_EVENT, 2)
          .put(REMEDIATION_PULL_REQUEST_EVENT, 1)
          .put(REPOSITORY_URL_UPDATED_EVENT, -1)
          .put(SOURCE_CONTROL_EVALUATION_EVENT, 8)
          .put(STATUS_UPDATE_EVENT, 8)
          .put(UPDATED_PULL_REQUEST_EVENT, 1)
          .build();

  /**
   * to better take advantage of the available event processing bandwidth we can flex the allowed event counts up
   * in case there are a lot of events of the same event type
   */
  private static final ImmutableMap<String, Integer> EXTRA_SIMULTANEOUS_EVENTS_ALLOWED =
      ImmutableMap.<String, Integer>builder()
          .put(APPLICATION_EVALUATION_EVENT, 2)
          .put(DISCOVERED_PULL_REQUEST_EVENT, 1)
          .put(REMEDIATION_PULL_REQUEST_EVENT, 1)
          .put(REPOSITORY_URL_UPDATED_EVENT, -1)
          .put(SOURCE_CONTROL_EVALUATION_EVENT, 2)
          .put(STATUS_UPDATE_EVENT, 2)
          .put(UPDATED_PULL_REQUEST_EVENT, 1)
          .build();

  /**
   * determines whether or not the given event can be pushed using the simultaneous events allowed count for the
   * given event's type.  However, there may be cases where the event types to process might be limited so we
   * want to be able to take advantage of the available event processing bandwidth.  Therefore, the strict parameter
   * is made available to give the client the ability to more events of the same type when conditions warrant.
   *
   * @param useStrictCounts when true the defined maximum simultaneous event counts are used;  otherwise,
   *                        some additional simultaneous events are allowed
   */
  public boolean canPushEvent(
      SourceControlEvent event,
      Map<String, SourceControlEvent> eventsInProgress,
      boolean useStrictCounts)
  {
    if (null == event) {
      return false;
    }
    boolean result = computeCanPushEvent(event, eventsInProgress, useStrictCounts);
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  private boolean computeCanPushEvent(
      SourceControlEvent event,
      Map<String, SourceControlEvent> eventsInProgress,
      boolean useStrictCounts)
  {
    if (MapUtils.isEmpty(eventsInProgress)) {
      return true;
    }
    int allowedEventCount = getAllowedEventCount(event, useStrictCounts);
    if (allowedEventCount < 0) {
      // there is no limit on the count
      return true;
    }
    long similarEvents = countSimilarEvents(event, eventsInProgress);
    return similarEvents < allowedEventCount;
  }

  @VisibleForTesting
  int getAllowedEventCount(SourceControlEvent event, boolean useStrictCounts) {
    return SIMULTANEOUS_EVENTS_ALLOWED.get(event.getEventType())
        + (useStrictCounts ? 0 : EXTRA_SIMULTANEOUS_EVENTS_ALLOWED.get(event.getEventType()));
  }

  private long countSimilarEvents(SourceControlEvent event, Map<String, SourceControlEvent> eventsInProgress) {
    return eventsInProgress.values().stream().filter(e -> e.getEventType().equals(event.getEventType())).count();
  }
}
