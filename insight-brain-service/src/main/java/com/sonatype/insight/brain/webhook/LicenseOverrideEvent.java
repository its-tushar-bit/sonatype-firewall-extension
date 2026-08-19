/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;
import com.sonatype.insight.brain.model.license.LicenseOverride;

/**
 * @since 1.25.0
 */
public class LicenseOverrideEvent
    extends WebhookEvent
{
  public EventAction action;

  public LicenseOverride licenseOverride;

  @Override
  public String toString() {
    return getClass().getName() + "{licenseOverrideId=" +
        (licenseOverride != null ? licenseOverride.getId() : "null") +
        "}";
  }
}
