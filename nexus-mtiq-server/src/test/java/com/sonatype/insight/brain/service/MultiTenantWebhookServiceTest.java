/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.EnumSet;

import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.error.exception.BadRequestException;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

public class MultiTenantWebhookServiceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private WebhookDAO webhookDAO;

  private MultiTenantWebhookService webhookService;

  private PlexusCipher plexusCipher;

  private Configuration configuration;

  @Before
  public void setup() {
    webhookDAO = lookup(WebhookDAO.class);
    configuration = lookup(Configuration.class);
    plexusCipher = lookup(PlexusCipher.class);
    webhookService = (MultiTenantWebhookService) getTestCLMServer().getCLMServer().getInstance(WebhookService.class);
  }

  @Test
  public void testAddWebhook_http_BadRequestException() {
    final String secretKey = "some secret key";
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    //Throws bad request exception
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> webhookService.addWebhookNoAuthz(webhook));
  }

  @Test
  public void testAddWebhook_https() throws PlexusCipherException {
    final String secretKey = "some secret key";
    Webhook webhook = new Webhook();
    webhook.setUrl("https://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook = webhookService.addWebhookNoAuthz(webhook);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());
    assertThat(webhook.getSecretKey()).isNotEqualTo(secretKey);

    synchronized (plexusCipher) {
      final String decryptedSecretKey = plexusCipher
          .decrypt(webhook.getSecretKey(), configuration.getWebhookSecretPassphrase());
      assertThat(decryptedSecretKey).isEqualTo(secretKey);
    }

    webhookDAO.delete(webhook);
  }
}
