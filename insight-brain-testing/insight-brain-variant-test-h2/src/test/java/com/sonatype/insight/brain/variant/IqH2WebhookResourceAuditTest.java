/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.configuration.webhook.WebhookResource;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Converted from the legacy {@code WebhookResourceAuditTest}.
 */
@IqH2Test
public class IqH2WebhookResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  public void testAddWebhook() throws Exception {
    Webhook webhook = webhookRequest().query("context=lifecycle").body(webhook()).post().getBody(Webhook.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testAddWebhook_NullEventTypes() throws Exception {
    Webhook webhook = webhookRequest().query("context=lifecycle").body(webhook(null)).post().getBody(Webhook.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_WEBHOOK, null);
    assertWebhookData(auditDTO, webhook);
  }

  @Test
  public void testAddWebhook_Unauthorized() throws Exception {
    webhookRequest().query("context=lifecycle").body(webhook()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_WEBHOOK, "unauthorized");
  }

  @Test
  public void testUpdateWebhook() throws Exception {
    Webhook webhook = webhook();
    webhook.setId(ctx.tempEntity().newWebhook(Collections.emptySet()).getId());

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
    Webhook webhook = ctx.tempEntity().newWebhook(Collections.emptySet());

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
    return ctx.restRequest().path(WebhookResource.RESOURCE_PATH);
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

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
