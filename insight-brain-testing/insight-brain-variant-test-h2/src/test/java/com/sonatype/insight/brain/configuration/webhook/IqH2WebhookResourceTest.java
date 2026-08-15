/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.configuration.webhook.WebhookResource.WEBHOOK_EVENT_TYPES_PATH;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_ENCRYPTED;
import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.ORG_APP_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_ALERT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.WAIVER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kept in {@code WebhookResource}'s own package (rather than {@code com.sonatype.insight.brain.variant}) because
 * this test references the package-private {@link WebhookResource#POLICY_NOTIFICATION_WEBHOOKS_PATH}.
 */
@IqH2Test
class IqH2WebhookResourceTest
{
  private IqTestContext ctx;

  private WebhookDAO webhookDao;

  @BeforeEach
  void setUp() {
    webhookDao = ctx.lookup(WebhookDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(WebhookResource.RESOURCE_PATH);
  }

  @Test
  void testGetPolicyNotificationWebhooks_Organization() throws Exception {
    Webhook webhook = ctx.tempEntity().newWebhook(Collections.singleton(POLICY_ALERT));
    ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().path(WebhookResource.POLICY_NOTIFICATION_WEBHOOKS_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID)
        .get();
    ctx.assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    assertThat(results).extracting(Webhook::getId).containsExactly(webhook.getId());
  }

  @Test
  void testGetPolicyNotificationWebhooks_Application() throws Exception {
    Webhook webhook = ctx.tempEntity().newWebhook(Collections.singleton(POLICY_ALERT));
    ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().path(WebhookResource.POLICY_NOTIFICATION_WEBHOOKS_PATH)
        .parameter(OwnerType.APPLICATION, ctx.tempEntity().newApplicationWithParent().getPublicId())
        .get();
    ctx.assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    assertThat(results).extracting(Webhook::getId).containsExactly(webhook.getId());
  }

  @Test
  void testGetAll_ReturnsEmptyArrayWhenNoHooksExist() throws Exception {
    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);
    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results).isEmpty();
  }

  @Test
  void testGetAll_ReturnsAllWebhooks() throws Exception {
    Webhook webhook1 = ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook2 = ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook3 = ctx.tempEntity().newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results).extracting(Webhook::getId)
        .containsExactlyInAnyOrder(webhook1.getId(), webhook2.getId(),
            webhook3.getId());
  }

  @Test
  void testGetAllWebhookEventTypes_ReturnsAllWebhookEventTypes() throws Exception {
    // Without org app management webhook enabled
    HttpResponse response = restRequest().path(WEBHOOK_EVENT_TYPES_PATH).get();
    ctx.assertResponseStatus(200, response);

    WebhookEventType[] results = response.getBody(WebhookEventType[].class);

    assertThat(results).containsExactly(POLICY_MANAGEMENT, APPLICATION_EVALUATION, POLICY_ALERT,
        LICENSE_OVERRIDE_MANAGEMENT, SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT, WAIVER_REQUEST, ORG_APP_MANAGEMENT);
  }

  @Test
  void testGetAllWebhookEventTypes_AreSentAsDescriptiveName() throws Exception {
    ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    String result = response.getBodyText();
    assertThat(result).contains("Management");
  }

  @Test
  void testGetAll_SecretKeyIsNotInGetResponse() throws Exception {
    ctx.tempEntity().newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    ctx.assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    Webhook result = results[0];

    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);
  }

  @Test
  void testAddWebhook() throws Exception {
    Webhook webhook = new Webhook();
    webhook.setUrl("http://sonatype.com");
    webhook.setSecretKey("sooper_sekrit");
    webhook.setEventTypes(new HashSet<>(Arrays.asList(POLICY_MANAGEMENT, LICENSE_OVERRIDE_MANAGEMENT)));

    HttpResponse response = restRequest().query("context=lifecycle").body(webhook).post();
    ctx.assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);
    webhookDao.delete(result);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getEventTypes()).containsExactlyInAnyOrder(POLICY_MANAGEMENT, LICENSE_OVERRIDE_MANAGEMENT);
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);
  }

  @Test
  void testUpdateWebhook_DoesNotUpdateSecretKeyIfNotIncluded() throws Exception {
    Webhook webhook = ctx.tempEntity().newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("http://some-other.url");
    webhook.setSecretKey(FAKE_SECRET_KEY);

    HttpResponse response = restRequest().body(webhook).put();
    ctx.assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_ENCRYPTED);
  }

  @Test
  void testUpdateWebhook_UpdatesSecretKeyIfIncluded() throws Exception {
    Webhook webhook = ctx.tempEntity().newWebhook("http://localhost:3000", Collections.singleton(POLICY_MANAGEMENT));

    webhook.setUrl("http://localhost:8000");
    final String secretKey = WEBHOOK_SECRET_KEY_CLEAR;
    webhook.setSecretKey(secretKey);

    HttpResponse response = restRequest().body(webhook).put();
    ctx.assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey()).isNotEqualTo(secretKey).isNotEmpty();
  }

  @Test
  void testDeleteWebhook() throws Exception {
    Webhook webhook = ctx.tempEntity().newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().body(webhook).path(webhook.getId()).delete();
    ctx.assertResponseStatus(204, response);
    assertThat(webhookDao.getById(webhook.getId())).isNull();
  }
}
