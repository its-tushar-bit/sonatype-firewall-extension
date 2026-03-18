/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.APPLICATION_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.BATCH_PR_STATE_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.CLOSE_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.DISCOVERED_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.MANUAL_REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.PR_STATE_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REPOSITORY_URL_UPDATED_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.SOURCE_CONTROL_EVALUATION_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.STATUS_UPDATE_EVENT;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.UPDATED_PULL_REQUEST_EVENT;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;

public class SimultaneousEventSelectionRule
{
  private static final Logger log = LoggerFactory.getLogger(SimultaneousEventSelectionRule.class);

  private final SimultaneousEventLimitDataTable simultaneousEventLimitDataTable;

  public SimultaneousEventSelectionRule(SourceControlProvider sourceControlProvider) {
    this.simultaneousEventLimitDataTable = new SimultaneousEventLimitDataTable(sourceControlProvider);
  }

  /**
   * determines whether or not the given event can be pushed using the simultaneous events allowed count for the
   * given event's type. However, there may be cases where the event types to process might be limited so we
   * want to be able to take advantage of the available event processing bandwidth. Therefore, the strict parameter
   * is made available to give the client the ability to process more events of the same type when conditions warrant.
   *
   * @param useStrictCounts when true the defined maximum simultaneous event counts are used; otherwise,
   *          some additional simultaneous events are allowed
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
    return simultaneousEventLimitDataTable.getAllowedEventCount(event, useStrictCounts);
  }

  private long countSimilarEvents(SourceControlEvent event, Map<String, SourceControlEvent> eventsInProgress) {
    return eventsInProgress.values().stream().filter(e -> e.getEventType().equals(event.getEventType())).count();
  }

  private static class SimultaneousEventLimitDataTable
  {
    private final Map<String, SimultaneousEventLimit> eventLimits = new HashMap<>();

    private final SourceControlProvider sourceControlProvider;

    private String eventTypePointer;

    private SimultaneousEventLimitDataTable(SourceControlProvider sourceControlProvider) {
      this.sourceControlProvider = sourceControlProvider;
      initialize();
    }

    /**
     * defines the number of events of the given type that can be processed simultaneously; this is an arbitrary
     * number but is based on the cost for the given event type; in other words, we want fewer higher cost events
     * to run simultaneously
     */
    private void initialize() {
      limit(APPLICATION_EVALUATION_EVENT, 2, 2)
          .limit(BATCH_PR_STATE_UPDATE_EVENT, 2, 1)
          .adjust(GITHUB, 1, 1)
          .limit(CLOSE_PULL_REQUEST_EVENT, 8, 2)
          .adjust(GITHUB, 1, 1)
          .limit(DISCOVERED_PULL_REQUEST_EVENT, 2, 1)
          .limit(MANUAL_REMEDIATION_PULL_REQUEST_EVENT, 4, 2)
          .adjust(GITHUB, 1, 1)
          .limit(PR_STATE_UPDATE_EVENT, 8, 2)
          .adjust(GITHUB, 1, 1)
          .limit(REMEDIATION_PULL_REQUEST_EVENT, 4, 2)
          .adjust(GITHUB, 1, 1)
          .limit(REPOSITORY_URL_UPDATED_EVENT, -1, -1)
          .limit(SOURCE_CONTROL_EVALUATION_EVENT, 8, 2)
          .limit(STATUS_UPDATE_EVENT, 8, 2)
          .limit(UPDATED_PULL_REQUEST_EVENT, 1, 2)
          .adjust(GITHUB, 1, 1);
    }

    int getAllowedEventCount(SourceControlEvent event, boolean useStrictCounts) {
      SimultaneousEventLimit eventLimit = eventLimits.get(event.getEventType());
      return eventLimit.allowed + (useStrictCounts ? 0 : eventLimit.extra);
    }

    private SimultaneousEventLimitDataTable limit(String eventType, int allowed, int extra) {
      eventTypePointer = eventType;
      eventLimits.put(eventType, new SimultaneousEventLimit(allowed, extra));
      return this;
    }

    private SimultaneousEventLimitDataTable adjust(
        SourceControlProvider provider,
        int allowed,
        int extra)
    {
      // filter out adjustments for providers we don't care about
      if (provider == sourceControlProvider) {
        eventLimits.put(eventTypePointer, new SimultaneousEventLimit(allowed, extra));
      }
      return this;
    }
  }

  private static class SimultaneousEventLimit
  {
    int allowed;

    int extra;

    SimultaneousEventLimit(int allowed, int extra) {
      this.allowed = allowed;
      this.extra = extra;
    }
  }
}
