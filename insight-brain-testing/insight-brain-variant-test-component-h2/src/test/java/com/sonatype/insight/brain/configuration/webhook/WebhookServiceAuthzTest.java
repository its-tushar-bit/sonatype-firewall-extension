/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class WebhookServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  public WebhookService webhookService;

  @Inject
  public WebhookDAO webhookDAO;

  @Test
  public void testGetAllWebhookEventTypes_Authorized() {
    grantConfigureSystemPermission();

    webhookService.getAllWebhookEventTypes("lifecycle");
  }

  @Test
  public void testGetAllWebhookEventTypes_Unauthorized() {
    login();

    assertThrows(UnauthorizedException.class, () -> webhookService.getAllWebhookEventTypes("lifecycle"));
  }

  @Test
  public void testGetAllWebhookEventTypes_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> webhookService.getAllWebhookEventTypes("lifecycle"));
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Authorized_Organization() {
    grantReadPermission(org.getId());
    webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, org.getId());
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Authorized_Application() {
    grantReadPermission(app.getId());
    webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Unauthorized_Organization() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Unauthorized_Application() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Unauthenticated_Organization() {
    assertThrows(UnauthenticatedException.class,
        () -> webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, org.getId()));
  }

  @Test
  public void testGetPolicyNotificationWebhooks_Unauthenticated_Application() {
    assertThrows(UnauthenticatedException.class,
        () -> webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION, app.getPublicId()));
  }

  @Test
  public void testGetAll_Authorized() {
    grantConfigureSystemPermission();

    webhookService.getAll();
  }

  @Test
  public void testGetAll_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class, () -> webhookService.getAll());
  }

  @Test
  public void testGetAll_Unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> webhookService.getAll());
  }

  @Test
  public void testAddWebhook_Authorized() {
    grantConfigureSystemPermission();

    Webhook webhook = webhookService.addWebhook(new Webhook("http://some.url", "secret key"), "lifecycle");

    webhookDAO.delete(webhook);
  }

  @Test
  public void testAddWebhook_Unauthorized() {
    login();
    assertThrows(UnauthorizedException.class,
        () -> webhookService.addWebhook(new Webhook("http://some.url", "secret key"), "lifecycle"));
  }

  @Test
  public void testAddWebhook_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> webhookService.addWebhook(new Webhook("http://some.url", "secret key"), "lifecycle"));
  }

  @Test
  public void testUpdateWebhook_Authorized() {
    grantConfigureSystemPermission();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    webhookService.updateWebhook(webhook, "lifecycle");
  }

  @Test
  public void testUpdateWebhook_Unauthorized() {
    login();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    assertThrows(UnauthorizedException.class, () -> webhookService.updateWebhook(webhook, "lifecycle"));
  }

  @Test
  public void testUpdateWebhook_Unauthenticated() {
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    assertThrows(UnauthenticatedException.class, () -> webhookService.updateWebhook(webhook, "lifecycle"));
  }

  @Test
  public void testDeleteWebhook_Authorized() {
    grantConfigureSystemPermission();

    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhookService.deleteWebhook(webhook.getId());
  }

  @Test
  public void testDeleteWebhook_Unauthorized() {
    login();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    assertThrows(UnauthorizedException.class, () -> webhookService.deleteWebhook(webhook.getId()));
  }

  @Test
  public void testDeleteWebhook_Unauthenticated() {
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    assertThrows(UnauthenticatedException.class, () -> webhookService.deleteWebhook(webhook.getId()));
  }
}
