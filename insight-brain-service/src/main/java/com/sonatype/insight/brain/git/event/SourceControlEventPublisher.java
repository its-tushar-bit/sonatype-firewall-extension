/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

@Named
@Singleton
public class SourceControlEventPublisher
{
  private final SourceControlEventDAO sourceControlEventDAO;

  @Inject
  public SourceControlEventPublisher(SourceControlEventDAO sourceControlEventDAO) {
    this.sourceControlEventDAO = sourceControlEventDAO;
  }

  /**
   * Persists the given event to the durable event queue (i.e. DB table)
   *
   * @param event the event to persist
   */
  public void publishEvent(SourceControlEvent event) {
    if (null != event) {
      sourceControlEventDAO.insert(event);
    }
  }

  /**
   * Clears the existing events for the relevant application and persists the given event to the durable event queue
   * (i.e. DB table)
   *
   * @param event the event to persist which also holds the application id for which events will be cleared
   */
  public void clearEventsForApplicationAndPublishEvent(SourceControlEvent event) {
    if (null != event) {
      sourceControlEventDAO.clearEventsAndInsert(event);
    }
  }

  public boolean doesRemediationEventExistForBranch(String applicationId, String branchName) {
    return sourceControlEventDAO.hasRemediationEventForBranch(applicationId, branchName);
  }
}
