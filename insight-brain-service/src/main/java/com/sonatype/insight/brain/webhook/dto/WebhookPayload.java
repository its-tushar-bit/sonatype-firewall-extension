/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

import java.util.Date;

/**
 * The base structure for all webhook payloads.
 *
 * @since 1.25.0
 */
public abstract class WebhookPayload
{
  /**
   * When the change occurred.
   */
  public Date timestamp;

  /**
   * Who initiated the change.
   */
  public String initiator;
}
