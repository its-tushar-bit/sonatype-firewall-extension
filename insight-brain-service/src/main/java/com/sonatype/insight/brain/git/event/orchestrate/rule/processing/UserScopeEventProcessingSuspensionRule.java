/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git.event.orchestrate.rule.processing;

import java.time.LocalDateTime;
import java.util.Set;

import com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControlEvent.REMEDIATION_PULL_REQUEST_EVENT;

/**
 * some errors, namely the github rate abuse related error, should cause us to temporarily suspend processing of
 * all applicable event types
 */
public class UserScopeEventProcessingSuspensionRule
{
  private static final Logger log = LoggerFactory.getLogger(UserScopeEventProcessingSuspensionRule.class);

  private static final Set<String> SCM_NOTIFICATION_EVENTS = ImmutableSet.of(REMEDIATION_PULL_REQUEST_EVENT);

  private int defaultSuspensionTimeInSeconds = 30;

  private LocalDateTime scmNotificationEventSuspensionExpiration = LocalDateTime.now();

  public boolean canPushEvent(SourceControlEvent event) {
    boolean result = !(isScmNotificationEvent(event) && areScmNotificationEventsSuspended());
    log.trace("Can push event {} for app {} = {}", event.getEventType(), event.getApplicationId(), result);
    return result;
  }

  public void onEventProcessingError(SourceControlEvent event, Exception e) {
    if (null != e && StringUtils.isNotBlank(e.getMessage()) && e.getMessage().contains("abuse detection")) {
      suspendScmNotificationEventProcessingForXSeconds(event, defaultSuspensionTimeInSeconds);
      log.debug("Notification event processing suspended for {} seconds", defaultSuspensionTimeInSeconds);
    }
  }

  @VisibleForTesting
  boolean areScmNotificationEventsSuspended() {
    return scmNotificationEventSuspensionExpiration.isAfter(LocalDateTime.now());
  }

  @VisibleForTesting
  boolean isScmNotificationEvent(SourceControlEvent event) {
    return SCM_NOTIFICATION_EVENTS.contains(event.getEventType());
  }

  private void suspendScmNotificationEventProcessingForXSeconds(SourceControlEvent event, int seconds) {
    scmNotificationEventSuspensionExpiration = LocalDateTime.now().plusSeconds(seconds);
    log.debug("SCM notification event processing suspended for {} seconds due to rate abuse detected for '{}'", seconds,
        event.getEventType());
  }

  @VisibleForTesting
  public UserScopeEventProcessingSuspensionRule setDefaultSuspensionTimeForTesting(int timeInSeconds) {
    defaultSuspensionTimeInSeconds = timeInSeconds;
    return this;
  }
}
