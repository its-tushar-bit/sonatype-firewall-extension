/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Set;

import com.sonatype.insight.brain.configuration.webhook.WebhookService;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;

@Named
@Singleton
public class MultiTenantWebhookService
    extends WebhookService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantWebhookService.class);

  @Inject
  public MultiTenantWebhookService(
      final Configuration configuration,
      final ProductLicense productLicense,
      final WebhookDAO webhookDao,
      final OrganizationApplicationManagementEventService organizationApplicationManagementEventService)
  {
    super(configuration, productLicense, webhookDao, organizationApplicationManagementEventService);
  }

  @Override
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Webhook addWebhook(Webhook webhook) {
    return addWebhookNoAuthz(webhook);
  }

  public Webhook addWebhookNoAuthz(Webhook webhook) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      log.debug("Not adding Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }

    if (webhook.getUrl().startsWith("http://")) {
      throw new BadRequestException("HTTPS is required for Webhook URLs");
    }

    encryptWebhookSecretKey(webhook);
    webhookDao.insert(webhook);
    final Set<WebhookEventType> eventTypes = webhook.getEventTypes();
    if (!CollectionUtils.isEmpty(eventTypes) && eventTypes.contains(WebhookEventType.ORG_APP_MANAGEMENT)) {
      organizationApplicationManagementEventService.postEvent();
    }
    auditWebhook(webhook);
    webhook.setSecretKey(FAKE_SECRET_KEY);
    return webhook;
  }
}
