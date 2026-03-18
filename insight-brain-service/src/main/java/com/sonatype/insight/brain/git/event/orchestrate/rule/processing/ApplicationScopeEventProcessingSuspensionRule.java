/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.sonatype.insight.brain.common.exception.ExceptionHelper;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * certain processing errors should cause temporary suspension of events for the application to which the
 * errored event pertains; for example, UnknownHostException could be due to a temporary problem resolving the
 * host for the repo URL provided for the associated application (as experienced during load testing)
 */
public class ApplicationScopeEventProcessingSuspensionRule
    implements EventProcessedListener
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationScopeEventProcessingSuspensionRule.class);

  private int networkingErrorSuspensionSeconds = 60;

  private int exclusiveAccessRequestTimeoutSuspensionSeconds = 30;

  private final Map<String, LocalDateTime> eventSuspensionExpirationMap = new HashMap<>();

  public boolean canPushEvent(SourceControlEvent event) {
    boolean result = !isEventProcessingSuspendedForApplication(event.getApplicationId());
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  public void onEventProcessingError(SourceControlEvent event, Exception e) {
    int suspensionSeconds;
    String errorType;

    if (ExceptionHelper.hasCauseOrSuppressedOfType(e, UnknownHostException.class)) {
      suspensionSeconds = networkingErrorSuspensionSeconds;
      errorType = "UnknownHostException";
    }
    else if (ExceptionHelper.hasCauseOrSuppressedOfType(e, SocketTimeoutException.class)) {
      suspensionSeconds = networkingErrorSuspensionSeconds;
      errorType = "SocketTimeoutException";
    }
    else if (ExceptionHelper.hasCauseOrSuppressedOfType(e, HttpResponseException.class)
        && ExceptionUtils.getStackTrace(e).contains("Bad Gateway"))
    {
      suspensionSeconds = networkingErrorSuspensionSeconds;
      errorType = "BadGateway";
    }
    else if (ExceptionHelper.hasCauseOrSuppressedOfType(e, ExclusiveAccessRequestTimeoutException.class)) {
      suspensionSeconds = exclusiveAccessRequestTimeoutSuspensionSeconds;
      errorType = "ExclusiveAccessRequestTimeoutException";
    }
    else {
      return;
    }

    suspendApplicationEventProcessingForXSeconds(event.getApplicationId(), suspensionSeconds);
    log.debug("Event processing for application {} event '{}' suspended for {} seconds due to {}.",
        event.getApplicationId(), event.getEventType(), suspensionSeconds, errorType);
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
    networkingErrorSuspensionSeconds = timeoutInSeconds;
    exclusiveAccessRequestTimeoutSuspensionSeconds = timeoutInSeconds;
    return this;
  }
}
