/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.EnumSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.TestLicenseManager;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.features.LicensedFeature;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class WebhookServiceTest
    extends AbstractComponentTest
{
  @Inject
  private WebhookService webhookService;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private TestLicenseManager testLicenseManager;

  @Test
  public void testAddWebhook_EncryptsSecretKey() throws PlexusCipherException {
    WebhookDAO webhookDAO = new WebhookDAO();

    final String secretKey = "some secret key";
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook = webhookService.addWebhook(webhook);

    // WebhookService should fake out secret key when returning from addWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store secret key encrypted
    assertThat(webhook.getSecretKey()).isNotEqualTo(secretKey);
    synchronized (plexusCipher) {
      final String decryptedSecretKey = plexusCipher
          .decrypt(webhook.getSecretKey(), insightConfig.getWebhookSecretPassphrase());
      assertThat(decryptedSecretKey).isEqualTo(secretKey);
    }

    webhookDAO.delete(webhook);
  }

  @Test
  public void testAddWebhook_Unlicensed_NotAllowed() {
    testLicenseManager.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> webhookService.addWebhook(webhook));
  }

  @Test
  public void testAddAndDeleteWebhook_RepositoryLicensed_Allowed() {
    testLicenseManager.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    Webhook addedWebhook = webhookService.addWebhook(webhook);
    webhookService.deleteWebhook(addedWebhook.getId());
  }

  @Test
  public void testAddAndDeleteWebhook_ApplicationLicensed_Allowed() {
    testLicenseManager.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    Webhook addedWebhook = webhookService.addWebhook(webhook);
    webhookService.deleteWebhook(addedWebhook.getId());
  }

  @Test
  public void testUpdateWebhook_EncryptsSecretKey() throws PlexusCipherException {
    WebhookDAO webhookDAO = new WebhookDAO();

    Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhook = webhookService.updateWebhook(webhook);

    // WebhookService should fake out secret key when returning from updateWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store secret key encrypted
    assertThat(webhook.getSecretKey()).isNotEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
    synchronized (plexusCipher) {
      final String decryptedSecretKey = plexusCipher
          .decrypt(webhook.getSecretKey(), insightConfig.getWebhookSecretPassphrase());
      assertThat(decryptedSecretKey).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
    }
  }

  @Test
  public void testUpdateWebhook_Unlicensed_NotAllowed() {
    testLicenseManager.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    assertThatExceptionOfType(InvalidLicenseException.class).isThrownBy(() -> webhookService.updateWebhook(webhook));
  }

  @Test
  public void testUpdateWebhook_ApplicationLicensed_Allowed() {
    testLicenseManager.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhookService.updateWebhook(webhook);
  }

  @Test
  public void testUpdateWebhook_RepositoryLicensed_Allowed() {
    testLicenseManager.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhookService.updateWebhook(webhook);
  }

  @Test
  public void testAddWebhook_EmptySecretKeyEncryptsEmpty() {
    WebhookDAO webhookDAO = new WebhookDAO();

    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey("");
    webhook = webhookService.addWebhook(webhook);

    // WebhookService should fake out secret key when returning from addWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store empty secret key as empty string
    assertThat(webhook.getSecretKey()).isEmpty();

    webhookDAO.delete(webhook);
  }

  @Test
  public void testGetDecrypted() {
    Webhook webhook = tempEntity.newWebhookWithSecret("http://localhost", EnumSet.of(APPLICATION_EVALUATION));

    Webhook result = webhookService.getDecrypted(webhook.getId());

    assertThat(result.getId()).isEqualTo(webhook.getId());
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
  }

  @Test
  public void testDeleteWebhook_Unlicensed_NotAllowed() {
    testLicenseManager.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> webhookService.deleteWebhook(webhook.getId()));
  }
}
