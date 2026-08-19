/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

@Named
@Singleton
public class SourceControlEventFinder
{
  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public SourceControlEventFinder(SourceControlEventDAO sourceControlEventDAO) {
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  public Map<String, SourceControlEvent> getPendingOrInProgressSourceControlEvaluationEvents() {
    Map<String, SourceControlEvent> applicationEventMap = new HashMap<>();
    List<SourceControlEvent> events = sourceControlEventDAO.getPendingOrInProgressSourceControlEvaluationEvents();
    events.forEach(event -> applicationEventMap.put(event.getApplicationId(), event));
    return applicationEventMap;
  }
}
