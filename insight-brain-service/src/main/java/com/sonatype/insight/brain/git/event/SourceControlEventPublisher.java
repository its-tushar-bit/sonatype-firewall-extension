/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlEventDAO;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventCreationListener;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.insight.brain.sourcecontrol.SourceControlUtils;
import com.sonatype.insight.brain.tenancy.TenantReference;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class SourceControlEventPublisher
{
  private final SourceControlEventDAO sourceControlEventDAO;

  private final SourceControlUtils sourceControlUtils;

  private final ApiConfigFeaturesService apiConfigFeaturesService;

  private TenantReference<SourceControlEventCreationListener> sourceControlEventCreationListener;

  @Inject
  public SourceControlEventPublisher(
      SourceControlEventDAO sourceControlEventDAO,
      SourceControlUtils sourceControlUtils,
      ApiConfigFeaturesService apiConfigFeaturesService)
  {
    this.sourceControlEventDAO = sourceControlEventDAO;
    this.sourceControlUtils = sourceControlUtils;
    this.apiConfigFeaturesService = apiConfigFeaturesService;
  }

  public void setSourceControlEventListener(SourceControlEventCreationListener sourceControlEventCreationListener) {
    this.sourceControlEventCreationListener = new TenantReference<>(() -> sourceControlEventCreationListener);
  }

  /**
   * Persists the given event to the durable event queue (i.e. DB table)
   *
   * @param event the event to persist
   */
  public void publishEvent(SourceControlEvent event) {
    if (null == event || !apiConfigFeaturesService.isSaasLifecycleScmEnabled()) {
      return;
    }
    doPublish(event);
  }

  /**
   * Persists an event without consulting the SaaS-Lifecycle-SCM feature gate. The relay
   * integration is itself an admin-enabled SCM ingestion path; if the relay flag is on the
   * customer has implicitly opted in to SCM event processing, and silently dropping events
   * would mask the relay being non-functional.
   *
   * <p>
   * <b>Restricted use:</b> the only legitimate caller is {@link
   * com.sonatype.insight.brain.relay.RelayPollingService}. Any new caller must verify it
   * has its own equivalent feature gate (e.g. an admin-managed configuration property)
   * before invoking this method — do not copy-paste a publish call from elsewhere.
   *
   * @param event the event to persist
   */
  public void publishEventBypassingFeatureGate(SourceControlEvent event) {
    if (null == event) {
      return;
    }
    doPublish(event);
  }

  private void doPublish(SourceControlEvent event) {
    populateScmUsernameIfMissing(event);
    sourceControlEventDAO.insert(event);
    if (null != sourceControlEventCreationListener && null != sourceControlEventCreationListener.get()) {
      sourceControlEventCreationListener.get().onNewEvent(event);
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

  private void populateScmUsernameIfMissing(SourceControlEvent event) {
    if (StringUtils.isBlank(event.getScmUsername())) {
      event.setScmUsername(sourceControlUtils.getScmUserIdForApplication(event.getApplicationId()));
    }
  }
}
