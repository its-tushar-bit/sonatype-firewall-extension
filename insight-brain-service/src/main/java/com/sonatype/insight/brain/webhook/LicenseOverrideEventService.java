/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.security.CurrentUser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.25.0
 */
@Singleton
@Named
public class LicenseOverrideEventService
{
  private static final Logger log = LoggerFactory.getLogger(LicenseOverrideEventService.class);

  private final AsyncEventBus asyncEventBus;

  private final CurrentUser currentUser;

  @Inject
  public LicenseOverrideEventService(
      final AsyncEventBus asyncEventBus,
      final CurrentUser currentUser)
  {
    this.asyncEventBus = asyncEventBus;
    this.currentUser = currentUser;
  }

  public void postEvent(
      final EventAction action,
      final LicenseOverride licenseOverride)
  {
    try {
      LicenseOverrideEvent event = new LicenseOverrideEvent();
      event.licenseOverride = licenseOverride;
      event.initiator = currentUser.getUsernameOrSystem();
      event.action = action;
      asyncEventBus.post(event);
    }
    catch (RuntimeException e) {
      log.error("Webhook not posted due to exception.", e);
    }
  }
}
