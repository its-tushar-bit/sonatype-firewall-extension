/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

public class WaiverRequestPayload
    extends WebhookPayload
{
  public String comment;

  public String policyViolationId;

  public String policyViolationLink;

  public String addWaiverLink;

  public String reasonId;

  public String reasonText;
}
