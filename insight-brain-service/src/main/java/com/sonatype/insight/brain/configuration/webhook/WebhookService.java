/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.brain.webhook.WebhookEventTypeDisplayUtil;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;

@Named
@Singleton
public class WebhookService
{
  private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

  private static final Set<WebhookEventType> FIREWALL_ONLY_EVENTS = Set.of(
      WebhookEventType.WAIVER_EXPIRATION);

  private static final Set<WebhookEventType> LIFECYCLE_ONLY_EVENTS = Set.of(
  // Future lifecycle-only events if needed
  );

  protected final WebhookDAO webhookDao;

  private final Configuration configuration;

  private final PlexusCipher plexusCipher;

  protected final ProductLicense productLicense;

  protected final OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Inject
  public WebhookService(
      final Configuration configuration,
      final ProductLicense productLicense,
      final WebhookDAO webhookDao,
      final OrganizationApplicationManagementEventService organizationApplicationManagementEventService)
  {
    this.configuration = configuration;
    this.productLicense = productLicense;
    this.webhookDao = webhookDao;
    this.organizationApplicationManagementEventService = organizationApplicationManagementEventService;
    this.plexusCipher = CipherFactory.createCipher();
  }

  @Authorize(permission = Permission.READ)
  List<Webhook> getPolicyNotificationWebhooks(
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.TYPE) OwnerType ownerType,
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ID) String ownerId)
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

  Long getWaiverRequestWebhooksCountNoAuthz() {
    return getAll_Unauthorized().stream()
        .filter(webhook -> webhook.getEventTypes().contains(WebhookEventType.WAIVER_REQUEST))
        .count();
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

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<Webhook> getAllFiltered(String context) {
    List<Webhook> allWebhooks = getAll(); // Returns all with redacted secrets

    // Filter webhooks based on their stored context
    // NULL context = old webhook created before context separation (backward compatibility)
    // These webhooks appear in current context regardless of which product context is requested
    return allWebhooks.stream()
        .filter(webhook -> {
          String webhookContext = webhook.getContext();
          // NULL webhooks (created before migration) appear in ALL contexts
          if (webhookContext == null) {
            return true;
          }
          // Explicitly classified webhooks only appear in their specific context
          return webhookContext.equalsIgnoreCase(context);
        })
        .collect(Collectors.toList());
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
  public List<String> getAllWebhookEventTypes(String context) {
    List<WebhookEventType> allEventTypes = new LinkedList<>(Arrays.asList(WebhookEventType.values()));

    boolean isFirewallContext = "firewall".equalsIgnoreCase(context);

    if (isFirewallContext) {
      // Firewall context: Remove Lifecycle-only events, check Firewall license
      if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES)) {
        log.debug("Firewall license not present, returning empty list");
        return Collections.emptyList();
      }
      allEventTypes.removeAll(LIFECYCLE_ONLY_EVENTS);
    }
    else {
      // Lifecycle context (default): Remove Firewall-only events, check Lifecycle license
      if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS)) {
        log.debug("Lifecycle license not present, returning empty list");
        return Collections.emptyList();
      }
      allEventTypes.removeAll(FIREWALL_ONLY_EVENTS);
    }

    // Map event types to display names based on context
    return allEventTypes.stream()
        .map(eventType -> WebhookEventTypeDisplayUtil.getContextualDisplayName(eventType, isFirewallContext))
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Webhook addWebhook(Webhook webhook, String context) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES))
    {
      log.debug("Not adding Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }

    // Context is required for new webhooks to ensure proper product-specific classification
    if (StringUtils.isBlank(context)) {
      throw new BadRequestException("Webhook context is required (firewall or lifecycle)");
    }

    // Store the context in the webhook (firewall or lifecycle)
    webhook.setContext(context);

    encryptWebhookSecretKey(webhook);
    webhookDao.insert(webhook);
    final Set<WebhookEventType> eventTypes = webhook.getEventTypes();
    if (!CollectionUtils.isEmpty(eventTypes) && eventTypes.contains(WebhookEventType.ORG_APP_MANAGEMENT)) {
      // Post context-specific event based on webhook's product context
      if ("firewall".equalsIgnoreCase(context)) {
        organizationApplicationManagementEventService.postEventForFirewall();
      }
      else {
        organizationApplicationManagementEventService.postEventForLifecycle();
      }
    }
    auditWebhook(webhook);
    webhook.setSecretKey(FAKE_SECRET_KEY);
    return webhook;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public Webhook updateWebhook(Webhook webhook, String context) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES))
    {
      log.debug("Not updating Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }

    // Re-classify webhook context on update. NULL-context webhooks (created before context separation)
    // are intentionally re-classified based on the UI context from which they're edited.
    // Frontend always provides explicit context; this default to "lifecycle" only applies to direct
    // API calls for backward compatibility. Once re-classified, the webhook will only appear in
    // and fire for the designated context.
    webhook.setContext(context != null ? context : "lifecycle");

    if (FAKE_SECRET_KEY.equals(webhook.getSecretKey())) {
      Webhook savedWebhook = webhookDao.getByIdNotNull(webhook.getId());
      webhook.setSecretKey(savedWebhook.getSecretKey());
    }
    else {
      encryptWebhookSecretKey(webhook);
    }
    final Webhook preUpdateWebhook = webhookDao.getById(webhook.getId());
    webhookDao.update(webhook);

    final Set<WebhookEventType> preUpdateEventTypes = preUpdateWebhook.getEventTypes();
    final Set<WebhookEventType> eventTypes = webhook.getEventTypes();
    // Check if this webhook already has org app summary included - if it does, don't post the payload
    if (!CollectionUtils.isEmpty(preUpdateEventTypes) &&
        !preUpdateEventTypes.contains(WebhookEventType.ORG_APP_MANAGEMENT) &&
        !CollectionUtils.isEmpty(eventTypes) &&
        eventTypes.contains(WebhookEventType.ORG_APP_MANAGEMENT))
    {
      // Post context-specific event based on webhook's product context
      if ("firewall".equalsIgnoreCase(webhook.getContext())) {
        organizationApplicationManagementEventService.postEventForFirewall();
      }
      else {
        organizationApplicationManagementEventService.postEventForLifecycle();
      }
    }
    auditWebhook(webhook);

    webhook.setSecretKey(FAKE_SECRET_KEY);
    return webhook;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteWebhook(String webhookId) {
    if (!productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS) &&
        !productLicense.hasFeature(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES))
    {
      log.debug("Not deleting Webhook, license does not support Webhooks.");
      throw new InvalidLicenseException();
    }
    Webhook webhook = webhookDao.getByIdNotNull(webhookId);
    webhookDao.delete(webhook);
    auditWebhook(webhook);
  }

  protected void auditWebhook(Webhook webhook) {
    List<String> webhookEventTypes =
        webhook.getEventTypes() == null
            ? new ArrayList<>()
            : webhook.getEventTypes()
                .stream()
                .map(webhookEventType -> webhookEventType.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .sorted()
                .collect(Collectors.toList());
    AuditData.get()
        .setData("webhookId", webhook.getId())
        .setData("webhookUrl", webhook.getUrl())
        .setData("webhookTriggerEvents", webhookEventTypes);
  }

  protected void encryptWebhookSecretKey(final Webhook webhook) {
    if (StringUtils.isNotEmpty(webhook.getSecretKey())) {
      synchronized (plexusCipher) {
        try {
          webhook.setSecretKey(
              plexusCipher.encrypt(webhook.getSecretKey(), configuration.getWebhookSecretPassphrase()));
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
              .decrypt(webhook.getSecretKey(), configuration.getWebhookSecretPassphrase()));
        }
        catch (PlexusCipherException e) {
          log.error("Unable to decrypt Webhook secret key", e);
          throw new IllegalStateException(e);
        }
      }
    }
  }
}
