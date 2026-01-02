/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import javax.inject.Inject;
import java.util.EnumSet;

import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_DEFAULT_WEBHOOK_SECRET_PASSPHRASE;
import static com.sonatype.insight.brain.security.FIPSConfig.FIPS_MODE_ENABLED_ENV;
import static com.sonatype.insight.brain.security.FipsTestUtil.insertBouncyCastleFipsProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
@RunWith(MockitoJUnitRunner.class)
public class MultiTenantWebhookServiceFIPSTest
    extends MultiTenantWebhookServiceTest
{
  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Mock
  private ProductLicense productLicense;

  @Inject
  private OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Before
  @Override
  public void setup() {
    // Ensure that the Bouncy Castle FIPS provider is inserted before the tests.
    insertBouncyCastleFipsProvider();

    // Initialize the EnvironmentVariables here instead of as a class variable as this gets run as part of a JUnit rule
    environmentVariables.set(FIPS_MODE_ENABLED_ENV, "true");
    super.setup();
  }

  @Test
  @Override
  public void testAddWebhook_https() throws PlexusCipherException {
    when(productLicense.hasFeature(any())).thenReturn(true);

    MultiTenantWebhookService webhookService = new MultiTenantWebhookService(configuration,
        productLicense, webhookDAO, organizationApplicationManagementEventService);

    PlexusCipher plexusCipher = CipherFactory.createCipher();

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
          .decrypt(webhook.getSecretKey(), FIPS_DEFAULT_WEBHOOK_SECRET_PASSPHRASE);
      assertThat(decryptedSecretKey).isEqualTo(secretKey);
    }

    webhookDAO.delete(webhook);
  }
}
