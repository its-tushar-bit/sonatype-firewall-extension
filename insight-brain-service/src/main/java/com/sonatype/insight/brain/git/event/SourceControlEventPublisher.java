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
import com.sonatype.insight.brain.git.SourceControlInstanceManager;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventCreationListener;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SourceControlEventPublisher
{
  private final ProductLicense productLicense;

  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlInstanceManager sourceControlInstanceManager;

  private SourceControlEventCreationListener sourceControlEventCreationListener;

  private final SourceControlUtils sourceControlUtils;

  @Inject
  public SourceControlEventPublisher(
      ProductLicense productLicense,
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlInstanceManager sourceControlInstanceManager,
      SourceControlUtils sourceControlUtils)
  {
    this.productLicense = productLicense;
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlInstanceManager = sourceControlInstanceManager;
    this.sourceControlUtils = sourceControlUtils;
  }

  public void setSourceControlEventListener(SourceControlEventCreationListener sourceControlEventCreationListener) {
    this.sourceControlEventCreationListener = sourceControlEventCreationListener;
  }

  /**
   * Persists the given event to the durable event queue (i.e. DB table)
   *
   * @param event the event to persist
   */
  public void publishEvent(SourceControlEvent event) {
    if (null != event && checkLicense()) {
      populateScmUsernameIfMissing(event);
      populateInstanceIdIfProcessingEvents(event);
      sourceControlEventDAO.insert(event);
      if (null != sourceControlEventCreationListener) {
        sourceControlEventCreationListener.onNewEvent(event);
      }
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

  private void populateInstanceIdIfProcessingEvents(SourceControlEvent event) {
    event.setInstanceId(sourceControlInstanceManager.canProcessEvents() ? sourceControlInstanceManager
        .getSourceControlInstanceId() : null);
  }

  private void populateScmUsernameIfMissing(SourceControlEvent event) {
    if (StringUtils.isBlank(event.getScmUsername())) {
      event.setScmUsername(sourceControlUtils.getScmUserIdForApplication(event.getApplicationId()));
    }
  }

  private boolean checkLicense() {
    return productLicense.hasFeature(LicensedFeature.AUTOMATION);
  }
}
