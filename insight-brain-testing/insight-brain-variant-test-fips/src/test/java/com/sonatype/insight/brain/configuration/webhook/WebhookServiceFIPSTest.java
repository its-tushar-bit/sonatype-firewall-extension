/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.EnumSet;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;

import org.sonatype.plexus.components.cipher.PlexusCipher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;

public class WebhookServiceFIPSTest
    extends WebhookServiceTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Inject
  private ProductLicense productLicense;

  @Inject
  private WebhookDAO webhookDAO;

  @Inject
  private OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  private static final String MOCKED_WEBHOOK_SECRET = "/$kin%&^@#{k0345";

  @Before
  public void setup() throws Exception {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
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
