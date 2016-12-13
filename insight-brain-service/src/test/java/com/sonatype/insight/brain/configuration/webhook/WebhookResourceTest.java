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
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.configuration.webhook.Webhook.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.configuration.webhook.WebhookResource.WEBHOOK_EVENT_TYPES_PATH;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_ENCRYPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class WebhookResourceTest
    extends AbstractResourceTest
{
  private WebhookDAO webhookDao = new WebhookDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(WebhookResource.RESOURCE_PATH);
  }

  @Test
  public void testGetAll_ReturnsEmptyArrayWhenNoHooksExist() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results.length, is(0));
  }

  @Test
  public void testGetAll_ReturnsAllWebhooks() throws Exception {
    Webhook webhook1 = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook2 = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));
    Webhook webhook3 = tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);

    assertThat(results.length, is(3));
    assertThat(results[0].getId(), is(webhook1.getId()));
    assertThat(results[1].getId(), is(webhook2.getId()));
    assertThat(results[2].getId(), is(webhook3.getId()));
  }

  @Test
  public void testGetAllWebhookEventTypes_ReturnsAllWebhookEventTypes() throws Exception {
    HttpResponse response = restRequest().path(WEBHOOK_EVENT_TYPES_PATH).get();
    assertResponseStatus(200, response);

    WebhookEventType[] results = response.getBody(WebhookEventType[].class);

    assertThat(results.length, is(4));
    assertThat(results[0], is(POLICY_MANAGEMENT));
    assertThat(results[1], is(APPLICATION_EVALUATION));
    assertThat(results[2], is(LICENSE_OVERRIDE_MANAGEMENT));
    assertThat(results[3], is(SECURITY_VULNERABILITY_OVERRIDE_MANAGEMENT));
  }

  @Test
  public void testGetAllWebhookEventTypes_AreSentAsDescriptiveName() throws Exception {
    tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    String result = response.getBodyText();
    assertThat(result, containsString("Management"));
  }

  @Test
  public void testGetAll_SecretKeyIsNotInGetResponse() throws Exception {
    tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);

    Webhook[] results = response.getBody(Webhook[].class);
    Webhook result = results[0];

    assertThat(result.getSecretKey(), is(FAKE_SECRET_KEY));
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

    assertThat(result.getId(), not(nullValue()));
    assertThat(result.getUrl(), is(webhook.getUrl()));
    assertThat(result.getEventTypes(), hasItems(POLICY_MANAGEMENT, LICENSE_OVERRIDE_MANAGEMENT));
    assertThat(result.getSecretKey(), is(FAKE_SECRET_KEY));
  }

  @Test
  public void testUpdateWebhook_DoesNotUpdateSecretKeyIfNotIncluded() throws Exception {
    Webhook webhook = tempEntity.newWebhookWithSecret(Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("http://some-other.url");
    webhook.setSecretKey(FAKE_SECRET_KEY);

    HttpResponse response = restRequest().body(webhook).put();
    assertResponseStatus(200, response);

    Webhook result = response.getBody(Webhook.class);

    assertThat(result.getId(), not(nullValue()));
    assertThat(result.getUrl(), is(webhook.getUrl()));
    assertThat(result.getSecretKey(), is(FAKE_SECRET_KEY));

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey(), is(WEBHOOK_SECRET_KEY_ENCRYPTED));
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

    assertThat(result.getId(), not(nullValue()));
    assertThat(result.getUrl(), is(webhook.getUrl()));
    assertThat(result.getSecretKey(), is(FAKE_SECRET_KEY));

    Webhook savedWebhook = webhookDao.getById(webhook.getId());
    assertThat(savedWebhook.getSecretKey(), is(not(secretKey)));
    assertThat(savedWebhook.getSecretKey(), not(isEmptyOrNullString()));
  }

  @Test
  public void testDeleteWebhook() throws Exception {
    Webhook webhook = tempEntity.newWebhook(Collections.singleton(POLICY_MANAGEMENT));

    HttpResponse response = restRequest().body(webhook).path(webhook.getId()).delete();
    assertResponseStatus(204, response);
    assertThat(webhookDao.getById(webhook.getId()), is(nullValue()));
  }
}
