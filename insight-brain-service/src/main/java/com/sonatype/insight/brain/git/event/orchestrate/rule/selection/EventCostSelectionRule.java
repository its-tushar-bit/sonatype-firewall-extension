/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.List;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.CollectionUtils;
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

public class EventCostSelectionRule
{
  private static final Logger log = LoggerFactory.getLogger(EventCostSelectionRule.class);

  public static final int GITHUB_MAX_IN_PROGRESS_EVENT_POINTS = 32;

  public static final int DEFAULT_MAX_IN_PROGRESS_EVENT_POINTS = 48;

  @VisibleForTesting
  final int maxInProgressEventPoints;

  /**
   * event point costs are arbitrary but at the same time are based on an analysis of the number of SCM interactions
   * that event type creates or the expected duration of that type of event. The points are relative to one another
   * with the status update event being the reference value since status update is a single, simple API call.
   *
   * Here's the thought that went into selecting the event points:
   * - status update : single API call
   * - source control evaluation : git pull + scan/evaluation
   * - remediation PR : clone + push + 2 API calls
   * - app eval : clone + multiple API calls (possible PR comment + multiple line comments)
   * - discovered PR : app eval activity + possible source control scans for source and target branches
   * - updated PR : discovered PR activity + possible delete line comment API calls
   */
  private static final ImmutableMap<String, Integer> EVENT_COST_POINTS = ImmutableMap.<String, Integer>builder()
      .put(APPLICATION_EVALUATION_EVENT, 8)
      .put(BATCH_PR_STATE_UPDATE_EVENT, 2)
      .put(CLOSE_PULL_REQUEST_EVENT, 2)
      .put(DISCOVERED_PULL_REQUEST_EVENT, 12)
      .put(MANUAL_REMEDIATION_PULL_REQUEST_EVENT, 4)
      .put(PR_STATE_UPDATE_EVENT, 1)
      .put(REMEDIATION_PULL_REQUEST_EVENT, 4)
      .put(REPOSITORY_URL_UPDATED_EVENT, 0)
      .put(SOURCE_CONTROL_EVALUATION_EVENT, 2)
      .put(STATUS_UPDATE_EVENT, 1)
      .put(UPDATED_PULL_REQUEST_EVENT, 12)
      .build();

  public static final int REMEDIATION_PR_EVENT_POINTS = EVENT_COST_POINTS.get(REMEDIATION_PULL_REQUEST_EVENT);

  public EventCostSelectionRule(SourceControlProvider sourceControlProvider) {
    maxInProgressEventPoints =
        GITHUB == sourceControlProvider ? GITHUB_MAX_IN_PROGRESS_EVENT_POINTS : DEFAULT_MAX_IN_PROGRESS_EVENT_POINTS;
  }

  /**
   * determines whether or not there are sufficient points available for the given event
   */
  public boolean canPushEvent(SourceControlEvent event, int availableEventPoints) {
    if (null == event) {
      return false;
    }
    boolean result = availableEventPoints >= getEventCost(event);
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  /**
   * given a list of events in progress this method calculates the total event cost for those events and subtracts
   * that total from the pre-defined maximum points for in-progress events to determine the remaining points available
   */
  public int getAvailableEventPoints(List<SourceControlEvent> eventsInProgress) {
    if (CollectionUtils.isEmpty(eventsInProgress)) {
      return maxInProgressEventPoints;
    }
    int availableEventPoints = maxInProgressEventPoints;
    for (SourceControlEvent event : eventsInProgress) {
      availableEventPoints -= getEventCost(event);
    }
    return availableEventPoints;
  }

  public int getEventCost(SourceControlEvent event) {
    if (null == event) {
      return 0;
    }
    return EVENT_COST_POINTS.get(event.getEventType());
  }
}
