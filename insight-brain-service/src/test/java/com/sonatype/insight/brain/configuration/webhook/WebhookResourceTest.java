/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import com.sonatype.insight.brain.common.test.SlowTest;

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
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

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

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class WebhookResourceTest
    extends AbstractResourceTest
{
  private WebhookDAO webhookDao;

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(WebhookResource.RESOURCE_PATH);
  }

  @Before
  public void setUp() {
    webhookDao = lookup(WebhookDAO.class);
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Organization() throws Exception {
    Webhook webhook = tempEntity.newWebhook(Collections.singleton(POLICY_ALERT));
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().path(WebhookResource.POLICY_NOTIFICATION_WEBHOOKS_PATH)
        .parameter(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID)
        .get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    assertThat(results).extracting(Webhook::getId).containsExactly(webhook.getId());
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Application() throws Exception {
    Webhook webhook = tempEntity.newWebhook(Collections.singleton(POLICY_ALERT));
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().path(WebhookResource.POLICY_NOTIFICATION_WEBHOOKS_PATH)
        .parameter(OwnerType.APPLICATION, tempEntity.newApplicationWithParent().getPublicId())
        .get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    assertThat(results).extracting(Webhook::getId).containsExactly(webhook.getId());
  }

  @Test
  public void testGetAll_ReturnsEmptyArrayWhenNoHooksExist() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetAll_ReturnsAllWebhooks() throws Exception {
    Webhook webhook1 = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook2 = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook3 = tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results).extracting(Webhook::getId)
        .containsExactly(webhook1.getId(), webhook2.getId(),
            webhook3.getId());
  }

  @Test
  public void testGetAllWebhookEventTypes_ReturnsAllWebhookEventTypes() throws Exception {
    // Without org app management webhook enabled
    HttpResponse response = restRequest().path(WEBHOOK_EVENT_TYPES_PATH).get();
    assertResponseStatus(200, response);

    WebhookEventType[] results = response.getBody(WebhookEventType[].class);

    assertThat(results).containsExactly(POLICY_MANAGEMENT, APPLICATION_EVALUATION, POLICY_ALERT,
        LICENSE_OVERRIDE_MANAGEMENT, SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT, WAIVER_REQUEST, ORG_APP_MANAGEMENT);
  }

  @Test
  public void testGetAllWebhookEventTypes_AreSentAsDescriptiveName() throws Exception {
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    String result = response.getBodyText();
    assertThat(result).contains("Management");
  }

  @Test
  public void testGetAll_SecretKeyIsNotInGetResponse() throws Exception {
    tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    Webhook result = results[0];

    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);
  }

  @Test
  public void testAddWebhook() throws Exception {
    Webhook webhook = new Webhook();
    webhook.setUrl("http://sonatype.com");
    webhook.setSecretKey("sooper_sekrit");
    webhook.setEventTypes(new HashSet<>(Arrays.asList(POLICY_MANAGEMENT, LICENSE_OVERRIDE_MANAGEMENT)));

    HttpResponse response = restRequest().body(webhook).post();
    assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);
    webhookDao.delete(result);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getEventTypes()).containsExactlyInAnyOrder(POLICY_MANAGEMENT, LICENSE_OVERRIDE_MANAGEMENT);
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);
  }

  @Test
  public void testUpdateWebhook_DoesNotUpdateSecretKeyIfNotIncluded() throws Exception {
    Webhook webhook = tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("http://some-other.url");
    webhook.setSecretKey(FAKE_SECRET_KEY);

    HttpResponse response = restRequest().body(webhook).put();
    assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_ENCRYPTED);
  }

  @Test
  public void testUpdateWebhook_UpdatesSecretKeyIfIncluded() throws Exception {
    Webhook webhook = tempEntity.newWebhook("http://localhost:3000", Collections.singleton(POLICY_MANAGEMENT));

    webhook.setUrl("http://localhost:8000");
    final String secretKey = WEBHOOK_SECRET_KEY_CLEAR;
    webhook.setSecretKey(secretKey);

    HttpResponse response = restRequest().body(webhook).put();
    assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);

    assertThat(result.getId()).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey()).isNotEqualTo(secretKey).isNotEmpty();
  }

  @Test
  public void testDeleteWebhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().body(webhook).path(webhook.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(webhookDao.getById(webhook.getId())).isNull();
  }
}
