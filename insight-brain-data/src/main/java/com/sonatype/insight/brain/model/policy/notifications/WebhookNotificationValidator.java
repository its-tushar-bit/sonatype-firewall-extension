/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.ValidationResult;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class WebhookNotificationValidator
    extends NotificationValidator<WebhookNotification>
{
  @Override
  protected ValidationResult validate(final WebhookNotification webhookNotification) {
    ValidationResult validationResult = new ValidationResult();
    if (StringUtils.isBlank(webhookNotification.getWebhookId())) {
      validationResult.addError("Invalid Webhook notification: A valid webhook id is required");
    }
    return validationResult;
  }
}
