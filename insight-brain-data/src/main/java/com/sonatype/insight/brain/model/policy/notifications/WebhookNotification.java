/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.notifications;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.ValidationResult;
import com.sonatype.insight.brain.model.policy.actions.NotifyActionType;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.64.0
 */
public class WebhookNotification
    extends Notification
{
  private String webhookId;

  public WebhookNotification() {
    // primarily supports deserialization
  }

  public WebhookNotification(final String webhookId, final String... stageIds) {
    super(stageIds);
    setWebhookId(webhookId);
  }

  @Override
  protected void validate(final ValidationResult validationResult) {
    if (StringUtils.isBlank(webhookId)) {
      validationResult.addError("Invalid Webhook notification: A valid webhook id is required");
    }
  }

  @Override
  public Action toAction() {
    return Action.newNotifyAction(webhookId, NotifyActionType.TARGET_TYPE_WEBHOOK);
  }

  @Override
  protected void addToNotifications(final Notifications notifications) {
    notifications.getWebhookNotifications().add(this);
  }

  public String getWebhookId() {
    return webhookId;
  }

  public void setWebhookId(final String webhookId) {
    this.webhookId = webhookId;
  }

  @Override
  public String toString() {
    return "WebhookNotification [webhookId=" + webhookId + ", getStageIds()="
        + getStageIds() + "]";
  }
}
