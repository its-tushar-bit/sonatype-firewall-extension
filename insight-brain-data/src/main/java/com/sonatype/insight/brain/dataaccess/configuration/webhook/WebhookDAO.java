/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.webhook;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.policy.notifications.WebhookNotification;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.Webhook.WEBHOOK;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.WebhookEventType.WEBHOOK_EVENT_TYPE;

@Named
@Singleton
public class WebhookDAO
    extends AbstractOperationalSqlDAO<Webhook>
{
  private final PolicyDAO policyDAO;

  @Inject
  public WebhookDAO(final OperationalDataStore operationalDataStore, final PolicyDAO policyDAO) {
    super(operationalDataStore);
    this.policyDAO = policyDAO;
  }

  @Override
  public int insert(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);
    int inserted = super.insert(tx, webhook);
    insertEventTypes(tx, webhook.getId(), webhook.getEventTypes());
    return inserted;
  }

  @Override
  public void update(final TransactionContext tx, final Webhook webhook) {
    validate(webhook);
    super.update(tx, webhook);
    deleteEventTypes(tx, webhook.getId());
    insertEventTypes(tx, webhook.getId(), webhook.getEventTypes());
  }

  @Override
  public void delete(final TransactionContext tx, final Webhook webhook) {
    deleteEventTypes(tx, webhook.getId());
    super.delete(tx, webhook);
    updatePoliciesRemoveWebhook(tx, webhook);
  }

  @Override
  public Webhook getById(final TransactionContext tx, final String id) {
    Webhook webhook = super.getById(tx, id);
    if (webhook != null) {
      webhook.setEventTypes(fetchEventTypes(tx, id));
    }
    return webhook;
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

  private void updatePoliciesRemoveWebhook(final TransactionContext tx, final Webhook webhook) {
    Predicate<WebhookNotification> predicate = notification -> notification.getWebhookId().equals(webhook.getId());
    policyDAO.getAll(tx)
        .stream()
        .filter(policy -> policy.getNotifications().getWebhookNotifications().removeIf(predicate))
        .forEach(policy -> policyDAO.update(tx, policy));
  }

  private void insertEventTypes(
      final TransactionContext tx,
      final String webhookId,
      final Set<WebhookEventType> eventTypes)
  {
    if (eventTypes == null || eventTypes.isEmpty()) {
      return;
    }
    for (WebhookEventType eventType : eventTypes) {
      tx.dsl()
          .insertInto(WEBHOOK_EVENT_TYPE)
          .set(WEBHOOK_EVENT_TYPE.WEBHOOK_ID, webhookId)
          .set(WEBHOOK_EVENT_TYPE.EVENT_TYPE, eventType.name())
          .execute();
    }
  }

  private void deleteEventTypes(final TransactionContext tx, final String webhookId) {
    tx.dsl()
        .deleteFrom(WEBHOOK_EVENT_TYPE)
        .where(WEBHOOK_EVENT_TYPE.WEBHOOK_ID.eq(webhookId))
        .execute();
  }

  private Set<WebhookEventType> fetchEventTypes(final TransactionContext tx, final String webhookId) {
    List<String> eventTypeNames = tx.dsl()
        .select(WEBHOOK_EVENT_TYPE.EVENT_TYPE)
        .from(WEBHOOK_EVENT_TYPE)
        .where(WEBHOOK_EVENT_TYPE.WEBHOOK_ID.eq(webhookId))
        .fetchInto(String.class);
    if (eventTypeNames.isEmpty()) {
      return EnumSet.noneOf(WebhookEventType.class);
    }
    Set<WebhookEventType> eventTypes = EnumSet.noneOf(WebhookEventType.class);
    for (String name : eventTypeNames) {
      eventTypes.add(WebhookEventType.valueOf(name));
    }
    return eventTypes;
  }

  @Override
  public List<Webhook> getAll(final TransactionContext tx) {
    List<Webhook> webhooks = tx.dsl()
        .selectFrom(WEBHOOK)
        .orderBy(WEBHOOK.URL)
        .fetchInto(Webhook.class);
    for (Webhook webhook : webhooks) {
      webhook.setEventTypes(fetchEventTypes(tx, webhook.getId()));
    }
    return webhooks;
  }

  @Override
  public Table<?> getJooqTable() {
    return WEBHOOK;
  }

  @Override
  public Class<Webhook> getEntityClass() {
    return Webhook.class;
  }
}
