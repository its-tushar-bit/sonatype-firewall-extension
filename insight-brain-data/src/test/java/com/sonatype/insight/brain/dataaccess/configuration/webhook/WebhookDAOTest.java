/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.webhook;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WebhookDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "http://localhost:3000";

  private final WebhookDAO dao = new WebhookDAO();

  @Test
  public void testCRUD_Http() {
    Webhook webhook = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    Webhook result = dao.getById(webhook.getId());

    assertThat(result).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(webhook.getSecretKey());

    webhook.setUrl("http://some-other.url");
    dao.update(webhook);

    result = dao.getById(webhook.getId());

    assertThat(result).isNotNull();
    assertThat(result.getUrl()).isEqualTo("http://some-other.url");

    dao.delete(webhook);
    result = dao.getById(webhook.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testCRUD_Https() {
    Webhook webhook = tempEntity
        .newWebhookWithSecret("https://localhost:3000", Collections.singleton(POLICY_MANAGEMENT));
    Webhook result = dao.getById(webhook.getId());

    assertThat(result).isNotNull();
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(webhook.getSecretKey());

    webhook.setUrl("https://some-other.url");
    dao.update(webhook);

    result = dao.getById(webhook.getId());

    assertThat(result).isNotNull();
    assertThat(result.getUrl()).isEqualTo("https://some-other.url");

    dao.delete(webhook);
    result = dao.getById(webhook.getId());

    assertThat(result).isNull();
  }

  @Test
  public void testInsert_MissingURL() {
    assertThatThrownBy(() -> tempEntity.newWebhook("", Collections.singleton(POLICY_MANAGEMENT))).isInstanceOf(
        BadRequestException.class).hasMessage("Webhook URL is required");
  }

  @Test
  public void testInsert_NullURL() {
    assertThatThrownBy(() -> tempEntity.newWebhook(null, Collections.singleton(POLICY_MANAGEMENT))).isInstanceOf(
        BadRequestException.class).hasMessage("Webhook URL is required");
  }

  @Test
  public void testInsert_NonHttp() {
    assertThatThrownBy(
        () -> tempEntity.newWebhook("ftp://test.com", Collections.singleton(POLICY_MANAGEMENT))).isInstanceOf(
        BadRequestException.class).hasMessage("Webhook URL must start with http:// or https://");
  }

  @Test
  public void testInsert_BlankURL() {
    assertThatThrownBy(() -> tempEntity.newWebhook("   ", Collections.singleton(POLICY_MANAGEMENT))).isInstanceOf(
        BadRequestException.class).hasMessage("Webhook URL is required");
  }

  @Test
  public void testInsert_InvalidURL() {
    assertThatThrownBy(
        () -> tempEntity.newWebhook("http://boom crash", Collections.singleton(POLICY_MANAGEMENT))).isInstanceOf(
            BadRequestException.class)
        .hasMessage("Webhook URL is invalid: Illegal character in authority at index 7: http://boom crash");
  }

  @Test
  public void testUpdate_MissingURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("");

    assertThatThrownBy(() -> dao.update(webhook)).isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook URL is required");
  }

  @Test
  public void testUpdate_NullURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl(null);

    assertThatThrownBy(() -> dao.update(webhook)).isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook URL is required");
  }

  @Test
  public void testUpdate_BlankURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("   ");

    assertThatThrownBy(() -> dao.update(webhook)).isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook URL is required");
  }

  @Test
  public void testUpdate_NonHttp() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("ftp://test.com");

    assertThatThrownBy(() -> dao.update(webhook)).isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook URL must start with http:// or https://");
  }

  @Test
  public void testUpdate_InvalidURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("http://not valid");

    assertThatThrownBy(() -> dao.update(webhook)).isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook URL is invalid: Illegal character in authority at index 7: http://not valid");
  }

  @Test
  public void testUpdate_MultipleEventTypes() {
    Set<WebhookEventType> eventTypes = EnumSet
        .of(POLICY_MANAGEMENT, APPLICATION_EVALUATION, LICENSE_OVERRIDE_MANAGEMENT);
    Webhook webhook = tempEntity.newWebhook(VALID_URL, eventTypes);

    Webhook actual = dao.getByIdNotNull(webhook.getId());

    assertThat(actual.getEventTypes()).isEqualTo(eventTypes);
  }

  @Test
  public void getAll() {
    Webhook webhook1 = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(APPLICATION_EVALUATION));
    Webhook webhook2 = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(APPLICATION_EVALUATION));

    List<Webhook> results = dao.getAll();
    assertThat(results).extracting(Webhook::getUrl).containsExactly(webhook1.getUrl(), webhook2.getUrl());
  }
}
