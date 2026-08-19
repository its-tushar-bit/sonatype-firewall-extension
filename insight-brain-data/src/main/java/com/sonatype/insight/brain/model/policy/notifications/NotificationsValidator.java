/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.model.ValidationResult;

@Named
@Singleton
public class NotificationsValidator
{
  private final UserNotificationValidator userNotificationValidator;

  private final RoleNotificationValidator roleNotificationValidator;

  private final JiraNotificationValidator jiraNotificationValidator;

  private final WebhookNotificationValidator webhookNotificationValidator;

  @Inject
  public NotificationsValidator(
      final UserNotificationValidator userNotificationValidator,
      final RoleNotificationValidator roleNotificationValidator,
      final JiraNotificationValidator jiraNotificationValidator,
      final WebhookNotificationValidator webhookNotificationValidator)
  {
    this.userNotificationValidator = userNotificationValidator;
    this.roleNotificationValidator = roleNotificationValidator;
    this.jiraNotificationValidator = jiraNotificationValidator;
    this.webhookNotificationValidator = webhookNotificationValidator;
  }

  public ValidationResult validate(final Notifications notifications) {
    ValidationResult validationResult = new ValidationResult();

    for (UserNotification userNotification : notifications.getUserNotifications()) {
      validationResult.merge(userNotificationValidator.validate(userNotification));
    }

    for (RoleNotification roleNotification : notifications.getRoleNotifications()) {
      validationResult.merge(roleNotificationValidator.validate(roleNotification));
    }

    for (JiraNotification jiraNotification : notifications.getJiraNotifications()) {
      validationResult.merge(jiraNotificationValidator.validate(jiraNotification));
    }

    for (WebhookNotification webhookNotification : notifications.getWebhookNotifications()) {
      validationResult.merge(webhookNotificationValidator.validate(webhookNotification));
    }

    return validationResult;
  }
}
