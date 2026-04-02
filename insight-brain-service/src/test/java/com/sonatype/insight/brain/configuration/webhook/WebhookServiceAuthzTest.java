/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.webhook.WebhookDAO;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;

import com.google.common.collect.Sets;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;

@Category(SlowTest.class)
public class WebhookServiceAuthzTest
    extends AbstractServiceAuthzTest
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

  @Test(expected = UnauthorizedException.class)
  public void testGetAllWebhookEventTypes_Unauthorized() {
    login();

    webhookService.getAllWebhookEventTypes("lifecycle");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAllWebhookEventTypes_Unauthenticated() {
    webhookService.getAllWebhookEventTypes("lifecycle");
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

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyNotificationWebhooks_Unauthorized_Organization() {
    login();
    webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetPolicyNotificationWebhooks_Unauthorized_Application() {
    login();
    webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyNotificationWebhooks_Unauthenticated_Organization() {
    webhookService.getPolicyNotificationWebhooks(OwnerType.ORGANIZATION, org.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetPolicyNotificationWebhooks_Unauthenticated_Application() {
    webhookService.getPolicyNotificationWebhooks(OwnerType.APPLICATION, app.getPublicId());
  }

  @Test
  public void testGetAll_Authorized() {
    grantConfigureSystemPermission();

    webhookService.getAll();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetAll_Unauthorized() {
    login();
    webhookService.getAll();
  }

  @Test(expected = UnauthenticatedException.class)
  public void testGetAll_Unauthenticated() {
    webhookService.getAll();
  }

  @Test
  public void testAddWebhook_Authorized() {
    grantConfigureSystemPermission();

    Webhook webhook = webhookService.addWebhook(new Webhook("http://some.url", "secret key"));

    webhookDAO.delete(webhook);
  }

  @Test(expected = UnauthorizedException.class)
  public void testAddWebhook_Unauthorized() {
    login();
    webhookService.addWebhook(new Webhook("http://some.url", "secret key"));
  }

  @Test(expected = UnauthenticatedException.class)
  public void testAddWebhook_Unauthenticated() {
    webhookService.addWebhook(new Webhook("http://some.url", "secret key"));
  }

  @Test
  public void testUpdateWebhook_Authorized() {
    grantConfigureSystemPermission();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    webhookService.updateWebhook(webhook);
  }

  @Test(expected = UnauthorizedException.class)
  public void testUpdateWebhook_Unauthorized() {
    login();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    webhookService.updateWebhook(webhook);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testUpdateWebhook_Unauthenticated() {
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhook.setEventTypes(Sets.newHashSet(WebhookEventType.APPLICATION_EVALUATION));

    webhookService.updateWebhook(webhook);
  }

  @Test
  public void testDeleteWebhook_Authorized() {
    grantConfigureSystemPermission();

    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhookService.deleteWebhook(webhook.getId());
  }

  @Test(expected = UnauthorizedException.class)
  public void testDeleteWebhook_Unauthorized() {
    login();
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhookService.deleteWebhook(webhook.getId());
  }

  @Test(expected = UnauthenticatedException.class)
  public void testDeleteWebhook_Unauthenticated() {
    Webhook webhook = tempEntity.newWebhook(Sets.newHashSet(WebhookEventType.POLICY_MANAGEMENT));

    webhookService.deleteWebhook(webhook.getId());
  }
}
