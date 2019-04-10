/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;

/**
 * @since 1.64
 */
public class WebhookNotificationDTO
    extends NotificationDTO
{
  public final String webhookId;

  public WebhookNotificationDTO(WebhookNotification webhookNotification) {
    super("webhook", webhookNotification.getStageIds());
    webhookId = webhookNotification.getWebhookId();
  }
}
