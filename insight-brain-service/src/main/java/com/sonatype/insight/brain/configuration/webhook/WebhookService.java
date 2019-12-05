/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;

@Named
@Singleton
public class WebhookService
{
  private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

  private final WebhookDAO webhookDao = new WebhookDAO();

  private final InsightConfig insightConfig;

  private final PlexusCipher plexusCipher;

  private final ProductLicense productLicense;

  @Inject
  public WebhookService(final InsightConfig insightConfig,
                        final PlexusCipher plexusCipher,
                        final ProductLicense productLicense)
  {
    this.insightConfig = insightConfig;
    this.plexusCipher = plexusCipher;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  List<Webhook> getPolicyNotificationWebhooks(
      @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @AuthzContext(AuthzContext.Key.ID) String ownerId)
  {
    List<Webhook> result = new ArrayList<>();
    for (Webhook webhook : webhookDao.getAll()) {
      if (webhook.getEventTypes().contains(WebhookEventType.POLICY_ALERT)) {
        Webhook redacted = new Webhook();
        redacted.setId(webhook.getId());
        redacted.setUrl(webhook.getUrl());
        redacted.setDescription(webhook.getDescription());
        result.add(redacted);
      }
    }
    return result;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<Webhook> getAll() {
    List<Webhook> result = new ArrayList<>();
    for (Webhook webhook : webhookDao.getAll()) {
      webhook.setSecretKey(FAKE_SECRET_KEY);
      result.add(webhook);
    }
    return result;
  }

  public List<Webhook> getAll_Unauthorized() {
    List<Webhook> result = new ArrayList<>();
    for (Webhook webhook : webhookDao.getAll()) {
      webhook.setSecretKey(FAKE_SECRET_KEY);
      result.add(webhook);
    }
    return result;
  }

  public Webhook getDecrypted(String webhookId) {
    Webhook webhook = webhookDao.getByIdNotNull(webhookId);
    decryptWebhookSecretKey(webhook);
    return webhook;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<WebhookEventType> getAllWebhookEventTypes() {
    return Lists.newArrayList(WebhookEventType.values());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Webhook addWebhook(Webhook webhook) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      log.debug("Not adding Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }
    encryptWebhookSecretKey(webhook);
    webhookDao.insert(webhook);
    auditWebhook(webhook);
    webhook.setSecretKey(FAKE_SECRET_KEY);
    return webhook;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Webhook updateWebhook(Webhook webhook) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      log.debug("Not updating Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }
    if (FAKE_SECRET_KEY.equals(webhook.getSecretKey())) {
      Webhook savedWebhook = webhookDao.getByIdNotNull(webhook.getId());
      webhook.setSecretKey(savedWebhook.getSecretKey());
    }
    else {
      encryptWebhookSecretKey(webhook);
    }
    webhookDao.update(webhook);
    auditWebhook(webhook);

    webhook.setSecretKey(FAKE_SECRET_KEY);
    return webhook;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteWebhook(String webhookId) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
      log.debug("Not deleting Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }
    Webhook webhook = webhookDao.getByIdNotNull(webhookId);
    webhookDao.delete(webhook);
    auditWebhook(webhook);
  }

  private void auditWebhook(Webhook webhook) {
    List<String> webhookEventTypes =
        webhook.getEventTypes() == null ? new ArrayList<>() : webhook.getEventTypes().stream()
            .map(webhookEventType -> webhookEventType.name().toLowerCase(Locale.ROOT).replace('_', '-')).sorted()
            .collect(Collectors.toList());
    AuditData.get().setData("webhookId", webhook.getId()).setData("webhookUrl", webhook.getUrl())
        .setData("webhookTriggerEvents", webhookEventTypes);
  }

  private void encryptWebhookSecretKey(final Webhook webhook) {
    if (StringUtils.isNotEmpty(webhook.getSecretKey())) {
      synchronized (plexusCipher) {
        try {
          webhook.setSecretKey(
              plexusCipher.encrypt(webhook.getSecretKey(), insightConfig.getWebhookSecretPassphrase()));
        }
        catch (PlexusCipherException e) {
          log.error("Unable to encrypt Webhook secret key", e);
          throw new IllegalStateException(e);
        }
      }
    }
  }

  public void decryptWebhookSecretKey(final Webhook webhook) {
    if (StringUtils.isNotBlank(webhook.getSecretKey())) {
      synchronized (plexusCipher) {
        try {
          webhook.setSecretKey(plexusCipher
              .decrypt(webhook.getSecretKey(), insightConfig.getWebhookSecretPassphrase()));
        }
        catch (PlexusCipherException e) {
          log.error("Unable to decrypt Webhook secret key", e);
          throw new IllegalStateException(e);
        }
      }
    }
  }
}
