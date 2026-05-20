/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.webhook.OrganizationApplicationManagementEventService;
import com.sonatype.insight.license.model.LicensedFeature;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.junit.Test;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_CLEAR;
import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.WEBHOOK_SECRET_KEY_ENCRYPTED;
import static com.sonatype.insight.brain.model.configuration.webhook.Webhook.FAKE_SECRET_KEY;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.APPLICATION_EVALUATION;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.ORG_APP_MANAGEMENT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.POLICY_ALERT;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.WAIVER_EXPIRATION;
import static com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType.WAIVER_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WebhookServiceTest
    extends AbstractComponentTest
{
  @Inject
  private WebhookDAO webhookDAO;

  @Inject
  private WebhookService webhookService;

  @Inject
  protected Configuration configuration;

  @Inject
  private PlexusCipher plexusCipher;

  @Inject
  private TestProductLicense testProductLicense;

  @Inject
  private OrganizationApplicationManagementEventService organizationApplicationManagementEventService;

  @Test
  public void testGetPolicyNotificationWebhooks_Organization() {
    Webhook webhook1 = tempEntity.newWebhookWithSecret("http://web.hook",
        Collections.singleton(WebhookEventType.POLICY_ALERT), "test");
    tempEntity.newWebhook(Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));

    List<Webhook> webhooks =
        webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);
    assertThat(webhooks).hasSize(1);
    Webhook webhook = webhooks.get(0);
    assertThat(webhook.getId()).isEqualTo(webhook1.getId());
    assertThat(webhook.getUrl()).isEqualTo(webhook1.getUrl());
    assertThat(webhook.getDescription()).isEqualTo(webhook1.getDescription());
    assertThat(webhook.getSecretKey()).isNull();
    assertThat(webhook.getEventTypes()).isNull();
  }

  @Test
  public void testGetWaiverRequestWebhooks() {
    tempEntity.newWebhookWithSecret("http://web.hook",
        Collections.singleton(WebhookEventType.WAIVER_REQUEST), "test");
    tempEntity.newWebhookWithSecret("http://web.hook.other",
        Collections.singleton(WebhookEventType.WAIVER_REQUEST), "test 2");
    tempEntity.newWebhook(Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));

    Long waiverRequestWebhooksCount = webhookService.getWaiverRequestWebhooksCountNoAuthz();
    assertThat(waiverRequestWebhooksCount).isEqualTo(2);
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Application() {
    Webhook webhook1 = tempEntity.newWebhookWithSecret("http://web.hook",
        Collections.singleton(WebhookEventType.POLICY_ALERT), "test");
    tempEntity.newWebhook(Collections.singleton(WebhookEventType.POLICY_MANAGEMENT));

    List<Webhook> webhooks = webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION,
        tempEntity.newApplicationWithParent().getPublicId());
    assertThat(webhooks).hasSize(1);
    Webhook webhook = webhooks.get(0);
    assertThat(webhook.getId()).isEqualTo(webhook1.getId());
    assertThat(webhook.getUrl()).isEqualTo(webhook1.getUrl());
    assertThat(webhook.getDescription()).isEqualTo(webhook1.getDescription());
    assertThat(webhook.getSecretKey()).isNull();
    assertThat(webhook.getEventTypes()).isNull();
  }

  @Test
  public void testAddWebhook_EncryptsSecretKey() throws PlexusCipherException {
    final String secretKey = "some secret key";
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook = webhookService.addWebhook(webhook, "lifecycle");

    // WebhookService should fake out secret key when returning from addWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store secret key encrypted
    assertThat(webhook.getSecretKey()).isNotEqualTo(secretKey);
    synchronized (plexusCipher) {
      final String decryptedSecretKey = plexusCipher
          .decrypt(webhook.getSecretKey(), configuration.getWebhookSecretPassphrase());
      assertThat(decryptedSecretKey).isEqualTo(secretKey);
    }

    webhookDAO.delete(webhook);
  }

  @Test
  public void testAddWebhook_Unlicensed_NotAllowed() {
    testProductLicense.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> webhookService.addWebhook(webhook, "lifecycle"));
  }

  @Test
  public void testAddWebhook_PostOrgAppManagementListOnPolicyManagementEventType() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(ORG_APP_MANAGEMENT));
    webhookService.addWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy).postEventForLifecycle();
  }

  @Test
  public void testAddWebhook_DoNotPostOrgAppManagementListWhenPolicyManagementEventTypeDoesNotExist() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(WAIVER_REQUEST, POLICY_ALERT));
    webhookService.addWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  @Test
  public void testAddWebhook_DoNotPostOrgAppManagementListWhenEventTypesSetIsNull() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(null);
    webhookService.addWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  @Test
  public void testAddAndDeleteWebhook_RepositoryLicensed_Allowed() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    Webhook addedWebhook = webhookService.addWebhook(webhook, "lifecycle");
    webhookService.deleteWebhook(addedWebhook.getId());
  }

  @Test
  public void testAddAndDeleteWebhook_ApplicationLicensed_Allowed() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    final String secretKey = "some secret key";
    final Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey(secretKey);
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    Webhook addedWebhook = webhookService.addWebhook(webhook, "lifecycle");
    webhookService.deleteWebhook(addedWebhook.getId());
  }

  @Test
  public void testUpdateWebhook_EncryptsSecretKey() throws PlexusCipherException {
    Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhook = webhookService.updateWebhook(webhook, "lifecycle");

    // WebhookService should fake out secret key when returning from updateWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store secret key encrypted
    assertThat(webhook.getSecretKey()).isNotEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
    synchronized (plexusCipher) {
      final String decryptedSecretKey = plexusCipher
          .decrypt(webhook.getSecretKey(), configuration.getWebhookSecretPassphrase());
      assertThat(decryptedSecretKey).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
    }
  }

  @Test
  public void testUpdateWebhook_Unlicensed_NotAllowed() {
    testProductLicense.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> webhookService.updateWebhook(webhook, "lifecycle"));
  }

  @Test
  public void testUpdateWebhook_ApplicationLicensed_Allowed() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhookService.updateWebhook(webhook, "lifecycle");
  }

  @Test
  public void testUpdateWebhook_RepositoryLicensed_Allowed() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhookService.updateWebhook(webhook, "lifecycle");
  }

  @Test
  public void testUpdateWebhook_NullContext_ReClassifiesAsLifecycle() throws PlexusCipherException {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    // Create webhook with NULL context (simulating pre-migration webhook)
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey("secret");
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook.setContext(null);
    webhookDAO.insert(webhook);

    // Update webhook with NULL context parameter (simulating direct API call without context)
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);
    webhookService.updateWebhook(webhook, null);

    // Verify it was re-classified as lifecycle (default)
    Webhook savedWebhook = webhookDAO.getByIdNotNull(webhook.getId());
    assertThat(savedWebhook.getContext()).isEqualTo(Webhook.CONTEXT_LIFECYCLE);

    webhookDAO.delete(webhook);
  }

  @Test
  public void testAddWebhook_NullContext_ThrowsBadRequest() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    // Create webhook without context
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setSecretKey("secret");
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));

    // Verify that adding webhook with null context throws BadRequestException
    assertThatThrownBy(() -> webhookService.addWebhook(webhook, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Webhook context is required (firewall or lifecycle)");
  }

  @Test
  public void testUpdateWebhook_PostOrgAppManagementListWhenPolicyManagementEventTypeIsAdded() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);
    webhook.getEventTypes().add(ORG_APP_MANAGEMENT);

    webhookService.updateWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy).postEventForLifecycle();
  }

  @Test
  public void testUpdateWebhook_DoNotPostOrgAppManagementListWhenPolicyManagementEventTypeIsNotAdded() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);
    webhook.getEventTypes().add(WAIVER_REQUEST);

    webhookService.updateWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  @Test
  public void testUpdateWebhook_DoNotPostOrgAppManagementListWhenPolicyManagementEventTypeAlreadyExists() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(POLICY_ALERT, ORG_APP_MANAGEMENT));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);

    webhookService.updateWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  @Test
  public void testUpdateWebhook_DoNotPostOrgAppManagementListWhenPolicyManagementEventTypeIsRemoved() {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(POLICY_ALERT, ORG_APP_MANAGEMENT));
    webhook.setSecretKey(WEBHOOK_SECRET_KEY_CLEAR);
    webhook.getEventTypes().remove(ORG_APP_MANAGEMENT);

    webhookService.updateWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  protected void testUpdateWebhook_DoNotPostOrgAppManagementListWhenEventTypesSetIsNull(String secretKey) {
    final OrganizationApplicationManagementEventService orgAppManagementEventServiceSpy =
        spy(organizationApplicationManagementEventService);
    final WebhookService webhookService =
        new WebhookService(configuration, testProductLicense, webhookDAO,
            orgAppManagementEventServiceSpy);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", null);
    webhook.setSecretKey(secretKey);

    webhookService.updateWebhook(webhook, "lifecycle");

    verify(orgAppManagementEventServiceSpy, never()).postEvent();
  }

  @Test
  public void testUpdateWebhook_DoNotPostOrgAppManagementListWhenEventTypesSetIsNull() {
    testUpdateWebhook_DoNotPostOrgAppManagementListWhenEventTypesSetIsNull(WEBHOOK_SECRET_KEY_CLEAR);
  }

  @Test
  public void testAddWebhook_EmptySecretKeyEncryptsEmpty() {
    Webhook webhook = new Webhook();
    webhook.setUrl("http://localhost");
    webhook.setEventTypes(EnumSet.of(APPLICATION_EVALUATION));
    webhook.setSecretKey("");
    webhook = webhookService.addWebhook(webhook, "lifecycle");

    // WebhookService should fake out secret key when returning from addWebhook
    assertThat(webhook.getSecretKey()).isEqualTo(FAKE_SECRET_KEY);

    webhook = webhookDAO.getByIdNotNull(webhook.getId());

    // WebhookService should store empty secret key as empty string
    assertThat(webhook.getSecretKey()).isEmpty();

    webhookDAO.delete(webhook);
  }

  @Test
  public void testGetDecrypted() throws Exception {
    Webhook webhook = tempEntity.newWebhookWithSecret("http://localhost",
        EnumSet.of(APPLICATION_EVALUATION), null, WEBHOOK_SECRET_KEY_ENCRYPTED);

    Webhook result = webhookService.getDecrypted(webhook.getId());

    assertThat(result.getId()).isEqualTo(webhook.getId());
    assertThat(result.getUrl()).isEqualTo(webhook.getUrl());
    assertThat(result.getSecretKey()).isEqualTo(WEBHOOK_SECRET_KEY_CLEAR);
  }

  @Test
  public void testDeleteWebhook_Unlicensed_NotAllowed() {
    testProductLicense.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS,
        LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    final Webhook webhook = tempEntity.newWebhook("http://localhost", EnumSet.of(APPLICATION_EVALUATION));

    assertThatExceptionOfType(InvalidLicenseException.class)
        .isThrownBy(() -> webhookService.deleteWebhook(webhook.getId()));
  }

  @Test
  public void testGetAllWebhookEventTypes_FirewallContext_IncludesWaiverExpiration() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("firewall");

    assertThat(eventTypes).contains("Waiver Expiration");
  }

  @Test
  public void testGetAllWebhookEventTypes_LifecycleContext_ExcludesWaiverExpiration() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("lifecycle");

    assertThat(eventTypes).doesNotContain("Waiver Expiration");
  }

  @Test
  public void testGetAllWebhookEventTypes_FirewallContext_ReturnsContainerEvaluationDisplayName() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("firewall");

    assertThat(eventTypes).contains("Container Evaluation");
    assertThat(eventTypes).doesNotContain("Application Evaluation");
  }

  @Test
  public void testGetAllWebhookEventTypes_LifecycleContext_ReturnsApplicationEvaluationDisplayName() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("lifecycle");

    assertThat(eventTypes).contains("Application Evaluation");
    assertThat(eventTypes).doesNotContain("Container Evaluation");
  }

  @Test
  public void testGetAllFiltered_LifecycleContext_HidesWaiverExpirationWebhook() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    // Create firewall-only webhook (WAIVER_EXPIRATION)
    Webhook firewallWebhook = tempEntity.newWebhook("http://webhook-waiver", EnumSet.of(WAIVER_EXPIRATION));
    firewallWebhook.setContext(Webhook.CONTEXT_FIREWALL);
    webhookDAO.update(firewallWebhook);

    // Create lifecycle webhook (POLICY_ALERT)
    Webhook lifecycleWebhook = tempEntity.newWebhook("http://webhook-policy", EnumSet.of(POLICY_ALERT));
    lifecycleWebhook.setContext(Webhook.CONTEXT_LIFECYCLE);
    webhookDAO.update(lifecycleWebhook);

    List<Webhook> webhooks = webhookService.getAllFiltered("lifecycle");

    assertThat(webhooks).hasSize(1);
    assertThat(webhooks.get(0).getUrl()).isEqualTo("http://webhook-policy");
  }

  @Test
  public void testGetAllFiltered_FirewallContext_ShowsWaiverExpirationWebhook() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    // Create firewall webhook (WAIVER_EXPIRATION)
    Webhook firewallWebhook1 = tempEntity.newWebhook("http://webhook-waiver", EnumSet.of(WAIVER_EXPIRATION));
    firewallWebhook1.setContext(Webhook.CONTEXT_FIREWALL);
    webhookDAO.update(firewallWebhook1);

    // Create another firewall webhook (POLICY_ALERT)
    Webhook firewallWebhook2 = tempEntity.newWebhook("http://webhook-policy", EnumSet.of(POLICY_ALERT));
    firewallWebhook2.setContext(Webhook.CONTEXT_FIREWALL);
    webhookDAO.update(firewallWebhook2);

    List<Webhook> webhooks = webhookService.getAllFiltered("firewall");

    assertThat(webhooks).hasSize(2);
    assertThat(webhooks.stream().anyMatch(w -> w.getUrl().equals("http://webhook-waiver"))).isTrue();
  }

  @Test
  public void testGetAllWebhookEventTypes_FirewallContext_ExcludesViolationAlert() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("firewall");

    assertThat(eventTypes).doesNotContain("Violation Alert");
  }

  @Test
  public void testGetAllWebhookEventTypes_FirewallContext_ExcludesWaiverRequest() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("firewall");

    assertThat(eventTypes).doesNotContain("Waiver Request");
  }

  @Test
  public void testGetAllWebhookEventTypes_LifecycleContext_IncludesViolationAlert() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("lifecycle");

    assertThat(eventTypes).contains("Violation Alert");
  }

  @Test
  public void testGetAllWebhookEventTypes_LifecycleContext_IncludesWaiverRequest() {
    testProductLicense.setFeatures(LicensedFeature.WEBHOOKS_FOR_APPLICATIONS);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("lifecycle");

    assertThat(eventTypes).contains("Waiver Request");
  }

  @Test
  public void testGetAllWebhookEventTypes_FirewallContext_NoLicense_ReturnsEmptyList() {
    testProductLicense.setMissingFeatures(LicensedFeature.WEBHOOKS_FOR_REPOSITORIES);

    List<String> eventTypes = webhookService.getAllWebhookEventTypes("firewall");

    assertThat(eventTypes).isEmpty();
  }
}
