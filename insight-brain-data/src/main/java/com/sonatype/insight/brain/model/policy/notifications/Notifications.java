/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Action;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @since 1.21
 */
public class Notifications
{
  private List<UserNotification> userNotifications = new ArrayList<>();

  private List<RoleNotification> roleNotifications = new ArrayList<>();

  private List<JiraNotification> jiraNotifications = new ArrayList<>();

  private List<WebhookNotification> webhookNotifications = new ArrayList<>();

  public Notifications() {
  }

  public Notifications(Notification... notifications) {
    for (Notification notification : notifications) {
      add(notification);
    }
  }

  @JsonIgnore
  public List<? extends Notification> getAllNotifications() {
    List<Notification> notifications = new ArrayList<>(
        userNotifications.size() + roleNotifications.size() + jiraNotifications.size() + webhookNotifications.size());
    notifications.addAll(userNotifications);
    notifications.addAll(roleNotifications);
    notifications.addAll(jiraNotifications);
    notifications.addAll(webhookNotifications);
    return notifications;
  }

  public List<UserNotification> getUserNotifications() {
    return userNotifications;
  }

  public void setUserNotifications(List<UserNotification> userNotifications) {
    this.userNotifications = userNotifications != null ? userNotifications : new ArrayList<>();
  }

  public List<RoleNotification> getRoleNotifications() {
    return roleNotifications;
  }

  public void setRoleNotifications(List<RoleNotification> roleNotifications) {
    this.roleNotifications = roleNotifications != null ? roleNotifications : new ArrayList<>();
  }

  public List<JiraNotification> getJiraNotifications() {
    return jiraNotifications;
  }

  public void setJiraNotifications(final List<JiraNotification> jiraNotifications) {
    this.jiraNotifications = jiraNotifications != null ? jiraNotifications : new ArrayList<>();
  }

  public List<WebhookNotification> getWebhookNotifications() {
    return webhookNotifications;
  }

  public void setWebhookNotifications(final List<WebhookNotification> webhookNotifications) {
    this.webhookNotifications = webhookNotifications != null ? webhookNotifications : new ArrayList<>();
  }

  public void add(Notification notification) {
    notification.addToNotifications(this);
  }

  public Notifications getApplicable(String stageId, boolean continuousMonitoring) {
    Notifications notifications = new Notifications();
    for (UserNotification notification : userNotifications) {
      if (notification.isApplicable(stageId, continuousMonitoring)) {
        notifications.userNotifications.add(notification);
      }
    }
    for (RoleNotification notification : roleNotifications) {
      if (notification.isApplicable(stageId, continuousMonitoring)) {
        notifications.roleNotifications.add(notification);
      }
    }
    for (JiraNotification notification : jiraNotifications) {
      if (notification.isApplicable(stageId, continuousMonitoring)) {
        notifications.jiraNotifications.add(notification);
      }
    }

    for (WebhookNotification notification : webhookNotifications) {
      if (notification.isApplicable(stageId, continuousMonitoring)) {
        notifications.webhookNotifications.add(notification);
      }
    }
    return notifications;
  }

  public List<Action> toActions() {
    List<Action> actions = new ArrayList<>();
    for (Notification notification : getAllNotifications()) {
      actions.add(notification.toAction());
    }
    return actions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jiraNotifications, roleNotifications, userNotifications, webhookNotifications);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Notifications other = (Notifications) obj;
    return Objects.equals(jiraNotifications, other.jiraNotifications)
        && Objects.equals(roleNotifications, other.roleNotifications)
        && Objects.equals(userNotifications, other.userNotifications)
        && Objects.equals(webhookNotifications, other.webhookNotifications);
  }
}
