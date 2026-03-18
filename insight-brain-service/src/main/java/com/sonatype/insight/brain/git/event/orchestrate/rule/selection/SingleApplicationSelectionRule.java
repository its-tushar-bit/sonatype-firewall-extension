/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.selection;

import java.util.Map;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleApplicationSelectionRule
{
  private static final Logger log = LoggerFactory.getLogger(SingleApplicationSelectionRule.class);

  /**
   * since the event processor enforces that multiple events for the same application execute sequentially it
   * makes sense to only have one event per application in progress at any time; this allows events for other
   * applications to run in parallel
   */
  public boolean canPushEvent(SourceControlEvent event, Map<String, SourceControlEvent> eventsInProgress) {
    if (null == event) {
      return false;
    }
    boolean result = MapUtils.isEmpty(eventsInProgress) || !eventsInProgress.containsKey(event.getApplicationId());
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }
}
