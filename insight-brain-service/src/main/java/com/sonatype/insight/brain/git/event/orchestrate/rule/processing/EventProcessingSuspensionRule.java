/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Set;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;
import com.sonatype.nexus.scm.api.access.control.ExclusiveAccessRequestTimeoutException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;

public class EventProcessingSuspensionRule
{
  private static final Logger log = LoggerFactory.getLogger(EventProcessingSuspensionRule.class);

  private static final Set<String> SCM_NOTIFICATION_EVENTS = ImmutableSet.of(REMEDIATION_PULL_REQUEST_EVENT);

  private LocalDateTime eventSuspensionExpiration = LocalDateTime.now();

  private LocalDateTime scmNotificationEventSuspensionExpiration = LocalDateTime.now();

  public boolean canPushEvent(SourceControlEvent event) {
    boolean result =
        !(isEventProcessingSuspended() || (isScmNotificationEvent(event) && areScmNotificationEventsSuspended()));
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  public void onEventProcessingError(Exception e) {
    if (e instanceof UnknownHostException) {
      suspendEventProcessingForXSeconds(60);
    }
    else if (e instanceof ExclusiveAccessRequestTimeoutException) {
      suspendEventProcessingForXSeconds(30);
    }
    else if (e.getMessage().contains("abuse detection")) {
      suspendScmNotificationEventProcessingFoxXSeconds(30);
    }
  }

  public boolean isEventProcessingSuspended() {
    return eventSuspensionExpiration.isAfter(LocalDateTime.now());
  }

  @VisibleForTesting
  boolean areScmNotificationEventsSuspended() {
    return scmNotificationEventSuspensionExpiration.isAfter(LocalDateTime.now());
  }

  @VisibleForTesting
  boolean isScmNotificationEvent(SourceControlEvent event) {
    return SCM_NOTIFICATION_EVENTS.contains(event.getEventType());
  }

  private void suspendEventProcessingForXSeconds(int seconds) {
    eventSuspensionExpiration = LocalDateTime.now().plusSeconds(seconds);
    log.debug("Event processing suspended for {} seconds", seconds);
  }

  private void suspendScmNotificationEventProcessingFoxXSeconds(int seconds) {
    scmNotificationEventSuspensionExpiration = LocalDateTime.now().plusSeconds(seconds);
    log.debug("SCM notification event processing suspended for {} seconds", seconds);
  }
}
