/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import java.time.LocalDateTime;

import com.sonatype.insight.brain.model.configuration.webhook.WebhookEvent;

public class WaiverRequestEvent
    extends WebhookEvent
{
  public LocalDateTime timestamp;

  public String comment;

  public String policyViolationId;

  public String policyViolationLink;

  public String addWaiverLink;

  public String ownerId;

  @Override
  public String toString() {
    String jsonifiedFields =
        String.format("{policyViolationId='%s',timestamp='%s',comment='%s'}", policyViolationId, timestamp.toString(),
            comment);
    return getClass().getName() + jsonifiedFields;
  }
}
