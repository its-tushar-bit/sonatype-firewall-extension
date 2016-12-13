/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.security.CurrentUser;

/**
 * @since 1.25.0
 */
@Singleton
@Named
public class LicenseOverrideEventService
{
  private final AsyncEventBus asyncEventBus;

  private final CurrentUser currentUser;

  @Inject
  public LicenseOverrideEventService(final AsyncEventBus asyncEventBus,
                                     final CurrentUser currentUser)
  {
    this.asyncEventBus = asyncEventBus;
    this.currentUser = currentUser;
  }

  public void postEvent(final EventAction action,
                        final LicenseOverride licenseOverride)
  {
    LicenseOverrideEvent event = new LicenseOverrideEvent();
    event.licenseOverride = licenseOverride;
    event.initiator = currentUser.getUsernameOrSystem();
    event.action = action;
    asyncEventBus.post(event);
  }
}
