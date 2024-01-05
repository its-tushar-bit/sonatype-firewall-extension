/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import java.util.Objects;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;

/**
 * @since 1.64
 */
public class WebhookNotification
    extends Notification
{
  private String webhookId;

  public WebhookNotification() {
    // primarily supports deserialization
  }

  public WebhookNotification(String webhookId, String... stageIds) {
    super(stageIds);
    setWebhookId(webhookId);
  }

  public String getWebhookId() {
    return webhookId;
  }

  public void setWebhookId(String webhookId) {
    this.webhookId = webhookId;
  }

  @Override
  public Action toAction() {
    return Action.newNotifyAction(webhookId, NotifyActionType.TARGET_TYPE_WEBHOOK);
  }

  @Override
  protected void addToNotifications(Notifications notifications) {
    notifications.getWebhookNotifications().add(this);
  }

  @Override
  public String toString() {
    return "WebhookNotification [webhookId=" + webhookId + ", getStageIds()=" + getStageIds() + "]";
  }

  @Override
  public int hashCode() {
    return Objects.hash(webhookId);
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
    WebhookNotification other = (WebhookNotification) obj;
    return Objects.equals(webhookId, other.webhookId);
  }
}
