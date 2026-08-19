/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.policy.notifications.JiraNotification;
import com.sonatype.insight.brain.model.policy.notifications.Notification;
import com.sonatype.insight.brain.model.policy.notifications.Notifications;
import com.sonatype.insight.brain.model.policy.notifications.RoleNotification;
import com.sonatype.insight.brain.model.policy.notifications.UserNotification;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;

public abstract class NotificationDTO
{
  public final String notificationType;

  public final Set<String> stageIds = new TreeSet<>();

  NotificationDTO(
      final String notificationType,
      final Set<String> stageIds)
  {
    this.notificationType = notificationType;
    this.stageIds.addAll(stageIds);
  }

  public static NotificationDTO from(Notification notification) {
    if (notification instanceof UserNotification) {
      return new UserNotificationDTO((UserNotification) notification);
    }
    else if (notification instanceof RoleNotification) {
      return new RoleNotificationDTO((RoleNotification) notification);
    }
    else if (notification instanceof WebhookNotification) {
      return new WebhookNotificationDTO((WebhookNotification) notification);
    }
    else {
      return new JiraNotificationDTO((JiraNotification) notification);
    }
  }

  public static List<NotificationDTO> transcribe(final Notifications notifications) {
    return notifications.getAllNotifications().stream().map(NotificationDTO::from).collect(Collectors.toList());
  }
}
