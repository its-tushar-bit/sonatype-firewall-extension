/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.EnumSet;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.security.FipsTestUtil.enableFipsMode;
import static com.sonatype.insight.brain.security.FipsTestUtil.removeBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;

public class WebhookServiceFIPSTest
    extends WebhookServiceTest
{
  @Inject
  private ProductLicense productLicense;

  @Inject
  private WebhookDAO webhookDAO;

  @Inject
  private OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  private static final String MOCKED_WEBHOOK_SECRET = "/$kin%&^@#{k0345";

  @AfterEach
  @Override
  public void afterTest() {
    super.afterTest();

    // Remove the Bouncy Castle FIPS provider after the test; the parent afterTest() accesses providers.
    removeBouncyCastleFipsProvider();
  }

  @Override
  public TemporaryEntity createTemporaryEntity() {
    // Enable FIPS mode (insert the BouncyCastle FIPS provider + set FIPS_MODE_ENABLED) before the Spring
    // context and TemporaryEntity are created.
    enableFipsMode();

    return super.createTemporaryEntity();
  }

  @Test
  @Override
  public void testGetDecrypted() throws Exception {
    WebhookService webhookService = new WebhookService(
        configuration, productLicense, webhookDAO, organizationApplicationManagementEventService);
    PlexusCipher plexusCipher = CipherFactory.createCipher();

    String secretKeyEncrypted = plexusCipher.encrypt(MOCKED_WEBHOOK_SECRET,
        configuration.getWebhookSecretPassphrase());

    Webhook webhook = tempEntity.newWebhookWithSecret("http://localhost",
        EnumSet.of(APPLICATION_EVALUATION), null, secretKeyEncrypted);

    Webhook result = webhookService.getDecrypted(webhook.getId());

    assertThat(result.getId()).isEqualTo(webhook.getId());
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(MOCKED_WEBHOOK_SECRET);
  }
}
