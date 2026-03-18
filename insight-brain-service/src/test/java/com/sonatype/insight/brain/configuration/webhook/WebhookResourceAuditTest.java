/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.service.AbstractAuditTest;

import org.junit.Test;

public class WebhookResourceAuditTest
    extends AbstractAuditTest
{
  @Test
  public void testAddWebhook() throws Exception {
    Webhook webhook = webhookRequest().body(webhook()).post().getBody(Webhook.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testAddWebhook_NullEventTypes() throws Exception {
    Webhook webhook = webhookRequest().body(webhook(null)).post().getBody(Webhook.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testAddWebhook_Unauthorized() throws Exception {
    webhookRequest().body(webhook()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_WEBHOOK, "unauthorized");
  }

  @Test
  public void testUpdateWebhook() throws Exception {
    Webhook webhook = webhook();
    webhook.setId(tempEntity.newWebhook(Collections.emptySet()).getId());

    webhookRequest().body(webhook).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testUpdateWebhook_Unauthorized() throws Exception {
    webhookRequest().body(webhook()).with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.UPDATE_WEBHOOK, "unauthorized");
  }

  @Test
  public void testDeleteWebhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook(Collections.emptySet());

    webhookRequest().path(WebhookResource.WEBHOOK_ID).parameter(webhook.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testDeleteWebhook_Unauthorized() throws Exception {
    webhookRequest().path(WebhookResource.WEBHOOK_ID).parameter("webhookId").with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_WEBHOOK, "unauthorized");
  }

  private HttpRequest webhookRequest() {
    return restRequest().path(WebhookResource.RESOURCE_PATH);
  }

  private Webhook webhook() {
    return webhook(EnumSet.allOf(WebhookEventType.class));
  }

  private Webhook webhook(Set<WebhookEventType> webhookEventTypes) {
    return new Webhook("http://url", "secret", webhookEventTypes);
  }

  private void assertWebhookData(AuditDTO auditDTO, Webhook webhook) {
    assertCustomData(auditDTO, "webhookId", webhook.getId());
    assertCustomData(auditDTO, "webhookUrl", webhook.getUrl());
    List<String> webhookTriggerEvents =
        webhook.getEventTypes() == null
            ? new ArrayList<>()
            : webhook.getEventTypes()
                .stream()
                .map(webhookEventType -> webhookEventType.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .sorted()
                .collect(Collectors.toList());
    assertCustomData(auditDTO, "webhookTriggerEvents", webhookTriggerEvents);
  }
}
