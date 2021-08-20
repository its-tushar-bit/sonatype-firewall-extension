/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * certain processing errors should cause temporary suspension of events for the application to which the
 * errored event pertains;  for example, UnknownHostException could be due to a temporary problem resolving the
 * host for the repo URL provided for the associated application (as experienced during load testing)
 */
public class ApplicationScopeEventProcessingSuspensionRule
    implements EventProcessedListener
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationScopeEventProcessingSuspensionRule.class);

  private int unknownHostSuspensionSeconds = 60;

  private int exclusiveAccessRequestTimeoutSuspensionSeconds = 30;

  private final Map<String, LocalDateTime> eventSuspensionExpirationMap = new HashMap<>();

  public boolean canPushEvent(SourceControlEvent event) {
    boolean result = !isEventProcessingSuspendedForApplication(event.getApplicationId());
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  public void onEventProcessingError(SourceControlEvent event, Exception e) {
    if (e instanceof UnknownHostException) {
      suspendApplicationEventProcessingForXSeconds(event.getApplicationId(), unknownHostSuspensionSeconds);
      log.debug("Event processing for application {} event '{}' suspended for {} seconds due to UnknownHostException",
          event.getApplicationId(), event.getEventType(), unknownHostSuspensionSeconds);
    }
    else if (e instanceof ExclusiveAccessRequestTimeoutException) {
      suspendApplicationEventProcessingForXSeconds(event.getApplicationId(),
          exclusiveAccessRequestTimeoutSuspensionSeconds);
      log.debug(
          "Event processing for application {} event '{}' suspended for {} seconds due to " +
              "ExclusiveAccessRequestTimeoutException",
          event.getApplicationId(), event.getEventType(), exclusiveAccessRequestTimeoutSuspensionSeconds);
    }
  }

  @Override
  public void onEventProcessed(SourceControlEvent event) {
    eventSuspensionExpirationMap.remove(event.getApplicationId());
  }

  public boolean isEventProcessingSuspendedForApplication(String applicationId) {
    LocalDateTime eventSuspensionExpiration = eventSuspensionExpirationMap.get(applicationId);
    return null != eventSuspensionExpiration && eventSuspensionExpiration.isAfter(LocalDateTime.now());
  }

  private void suspendApplicationEventProcessingForXSeconds(String applicationId, int seconds) {
    LocalDateTime eventSuspensionExpiration = LocalDateTime.now().plusSeconds(seconds);
    eventSuspensionExpirationMap.put(applicationId, eventSuspensionExpiration);
    log.debug("Event processing for application {} suspended for {} seconds", applicationId, seconds);
  }

  @VisibleForTesting
  public ApplicationScopeEventProcessingSuspensionRule setTimeoutsForTesting(int timeoutInSeconds) {
    unknownHostSuspensionSeconds = timeoutInSeconds;
    exclusiveAccessRequestTimeoutSuspensionSeconds = timeoutInSeconds;
    return this;
  }
}
