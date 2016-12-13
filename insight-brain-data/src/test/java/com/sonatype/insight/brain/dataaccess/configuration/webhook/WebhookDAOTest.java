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

import com.sonatype.insight.brain.configuration.webhook.Webhook;
import com.sonatype.insight.brain.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.LICENSE_OVERRIDE_MANAGEMENT;
import static com.sonatype.insight.brain.configuration.webhook.WebhookEventType.POLICY_MANAGEMENT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class WebhookDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "http://localhost:3000";

  private final WebhookDAO dao = new WebhookDAO();

  @Test
  public void testCRUD_Http() {
    Webhook webhook = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    Webhook result = dao.getById(webhook.getId());

    assertThat(result, not(nullValue()));
    assertThat(result.getUrl(), equalTo(webhook.getUrl()));
    assertThat(result.getSecretKey(), equalTo(webhook.getSecretKey()));

    webhook.setUrl("http://some-other.url");
    dao.update(webhook);

    result = dao.getById(webhook.getId());

    assertThat(result, not(nullValue()));
    assertThat(result.getUrl(), equalTo("http://some-other.url"));

    dao.delete(webhook);
    result = dao.getById(webhook.getId());

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testCRUD_Https() {
    Webhook webhook = tempEntity
        .newWebhookWithSecret("https://localhost:3000", Collections.singleton(POLICY_MANAGEMENT));
    Webhook result = dao.getById(webhook.getId());

    assertThat(result, not(nullValue()));
    assertThat(result.getUrl(), equalTo(webhook.getUrl()));
    assertThat(result.getSecretKey(), equalTo(webhook.getSecretKey()));

    webhook.setUrl("https://some-other.url");
    dao.update(webhook);

    result = dao.getById(webhook.getId());

    assertThat(result, not(nullValue()));
    assertThat(result.getUrl(), equalTo("https://some-other.url"));

    dao.delete(webhook);
    result = dao.getById(webhook.getId());

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testInsert_MissingURL() {
    try {
      tempEntity.newWebhook("", Collections.singleton(POLICY_MANAGEMENT));
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testInsert_NullURL() {
    try {
      tempEntity.newWebhook(null, Collections.singleton(POLICY_MANAGEMENT));
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testInsert_NonHttp() {
    try {
      tempEntity.newWebhook("ftp://test.com", Collections.singleton(POLICY_MANAGEMENT));
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL must start with http:// or https://"));
    }
  }

  @Test
  public void testInsert_BlankURL() {
    try {
      tempEntity.newWebhook("   ", Collections.singleton(POLICY_MANAGEMENT));
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testInsert_InvalidURL() {
    try {
      tempEntity.newWebhook("http://boom crash", Collections.singleton(POLICY_MANAGEMENT));
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Webhook URL is invalid: Illegal character in authority at index 7: http://boom crash"));
    }
  }

  @Test
  public void testUpdate_MissingURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("");

    try {
      dao.update(webhook);
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testUpdate_NullURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl(null);

    try {
      dao.update(webhook);
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testUpdate_BlankURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("   ");

    try {
      dao.update(webhook);
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL is required"));
    }
  }

  @Test
  public void testUpdate_NonHttp() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("ftp://test.com");

    try {
      dao.update(webhook);
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is("Webhook URL must start with http:// or https://"));
    }
  }

  @Test
  public void testUpdate_InvalidURL() {
    Webhook webhook = tempEntity.newWebhook(VALID_URL, Collections.singleton(POLICY_MANAGEMENT));
    webhook.setUrl("http://not valid");

    try {
      dao.update(webhook);
      fail("Should have failed with BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Webhook URL is invalid: Illegal character in authority at index 7: http://not valid"));
    }
  }

  @Test
  public void testUpdate_MultipleEventTypes() {
    Set<WebhookEventType> eventTypes = EnumSet
        .of(POLICY_MANAGEMENT, APPLICATION_EVALUATION, LICENSE_OVERRIDE_MANAGEMENT);
    Webhook webhook = tempEntity.newWebhook(VALID_URL, eventTypes);

    Webhook actual = dao.getByIdNotNull(webhook.getId());

    assertThat(actual.getEventTypes(), is(eventTypes));
  }

  @Test
  public void getAll() {
    Webhook webhook1 = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(APPLICATION_EVALUATION));
    Webhook webhook2 = tempEntity.newWebhookWithSecret(VALID_URL, Collections.singleton(APPLICATION_EVALUATION));

    List<Webhook> results = dao.getAll();
    assertThat(results, hasSize(2));
    assertThat(results.get(0).getUrl(), equalTo(webhook1.getUrl()));
    assertThat(results.get(1).getUrl(), equalTo(webhook2.getUrl()));
  }
}
