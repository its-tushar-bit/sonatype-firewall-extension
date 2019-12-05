/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.25
 */
@Named
@Timed
@Path(value = WebhookResource.RESOURCE_PATH)
public class WebhookResource
{
  public static final String WEBHOOK_ID = "{webhookId}";

  public static final String RESOURCE_PATH = "rest/config/webhook";

  public static final String WEBHOOK_EVENT_TYPES_PATH = "eventTypes";

  static final String POLICY_NOTIFICATION_WEBHOOKS_PATH = "policy/{ownerType: application|organization}/{ownerId}";

  private final WebhookService webhookService;

  @Inject
  public WebhookResource(final WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  /**
   * @since 1.81
   */
  @GET
  @Path(POLICY_NOTIFICATION_WEBHOOKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<Webhook> getPolicyNotificationWebhooks(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId)
  {
    return webhookService.getPolicyNotificationWebhooks(ownerType, ownerId);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Webhook> getAll() {
    return webhookService.getAll();
  }

  @Path(WEBHOOK_EVENT_TYPES_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<WebhookEventType> getAllWebhookEventTypes() {
    return webhookService.getAllWebhookEventTypes();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WEBHOOK)
  public Webhook addWebhook(Webhook webhook) {
    return webhookService.addWebhook(webhook);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_WEBHOOK)
  public Webhook updateWebhook(Webhook webhook) {
    return webhookService.updateWebhook(webhook);
  }

  @DELETE
  @Path(WEBHOOK_ID)
  @Audited(AuditEvent.DELETE_WEBHOOK)
  public void deleteWebhook(@PathParam("webhookId") final String webhookId) {
    webhookService.deleteWebhook(webhookId);
  }
}
