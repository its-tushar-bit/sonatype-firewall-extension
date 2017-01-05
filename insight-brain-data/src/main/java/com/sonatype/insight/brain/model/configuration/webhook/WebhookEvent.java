/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.configuration.webhook;

/**
 * @since 1.25.0
 */
public abstract class WebhookEvent
{
  /**
   * Contains the username which triggered the event.
   */
  public String initiator;
}
