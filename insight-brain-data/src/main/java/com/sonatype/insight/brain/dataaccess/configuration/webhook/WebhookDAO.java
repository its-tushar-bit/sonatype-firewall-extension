/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Predicate;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;

@Named
@Singleton
public class WebhookDAO
    extends AbstractOperationalSqlDAO<Webhook>
{
  private final PolicyDAO policyDAO;

  @Inject
  public WebhookDAO(OperationalDataStore operationalDataStore, PolicyDAO policyDAO) {
    super(operationalDataStore);
    this.policyDAO = policyDAO;
  }

  @Override
  public void insert(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);

    super.insert(tx, webhook);
  }

  @Override
  public void update(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);

    super.update(tx, webhook);
  }

  @Override
  public void delete(TransactionContext tx, Webhook webhook) {
    super.delete(tx, webhook);
    updatePoliciesRemoveWebhook(tx, webhook);
  }

  private void validate(final Webhook webhook) {
    if (StringUtils.isBlank(webhook.getUrl())) {
      throw new BadRequestException("Webhook URL is required");
    }
    if (!webhook.getUrl().startsWith("http://") && !webhook.getUrl().startsWith("https://")) {
      throw new BadRequestException("Webhook URL must start with http:// or https://");
    }
    try {
      new URI(webhook.getUrl());
    }
    catch (URISyntaxException e) {
      throw new BadRequestException("Webhook URL is invalid: " + e.getMessage(), e);
    }
  }

  /**
   * Remove the webhook from the policies (if exists) and persist the policies
   */
  private void updatePoliciesRemoveWebhook(TransactionContext tx, Webhook webhook) {
    Predicate<WebhookNotification> predicate = notification -> notification.getWebhookId().equals(webhook.getId());
    policyDAO.getAll(tx).stream()
        .filter(policy -> policy.getNotifications().getWebhookNotifications().removeIf(predicate))
        .forEach(policy -> policyDAO.update(tx, policy));
  }
}

