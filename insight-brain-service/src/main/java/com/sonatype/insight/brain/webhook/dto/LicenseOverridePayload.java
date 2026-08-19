/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.webhook.EventAction;

/**
 * @since 1.25.0
 */
public class LicenseOverridePayload
    extends WebhookPayload
{
  public EventAction action;

  public String id;

  public LicenseOverrideDTO licenseOverride;

  public static class LicenseOverrideDTO
  {
    public String id;

    public String ownerId;

    public String status;

    public String comment;

    public Set<String> licenseIds;

    public ApiComponentIdentifierDTOV2 componentIdentifier;
  }
}
