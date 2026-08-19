/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.webhook;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.configuration.webhook.WebhookEventType;
import com.sonatype.insight.brain.webhook.WebhookContextHolder;

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

  static final String POLICY_NOTIFICATION_WEBHOOKS_PATH =
      "policy/{ownerType: application|organization|repository|repository_manager|repository_container}/{ownerId}";

  public static final String WAIVER_REQUEST_WEBHOOKS_PATH = "waiverRequestCount";

  private final WebhookService webhookService;

  @Inject
  public WebhookResource(final WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @GET
  @Path(POLICY_NOTIFICATION_WEBHOOKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<Webhook> getPolicyNotificationWebhooks(
      @PathParam("ownerType") OwnerType ownerType,
      @PathParam("ownerId") String ownerId,
      @QueryParam("eventType") @DefaultValue("POLICY_ALERT") WebhookEventType eventType)
  {
    return webhookService.getPolicyNotificationWebhooksByEventType(ownerType, ownerId, eventType);
  }

  /**
   * @since 1.165
   */
  @GET
  @Path(WAIVER_REQUEST_WEBHOOKS_PATH)
  @Produces(MediaType.TEXT_PLAIN)
  public Long getWaiverRequestWebhooksCount() {
    return webhookService.getWaiverRequestWebhooksCountNoAuthz();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Webhook> getAll(@QueryParam("context") @DefaultValue("lifecycle") String context) {
    // Set context in ThreadLocal for custom serializer to use during Jackson serialization
    WebhookContextHolder.setContext(context);
    try {
      return webhookService.getAllFiltered(context);
    }
    finally {
      // Clear ThreadLocal to prevent memory leaks and thread pollution in Dropwizard's thread pool
      WebhookContextHolder.clear();
    }
  }

  @Path(WEBHOOK_EVENT_TYPES_PATH)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getAllWebhookEventTypes(
      @QueryParam("context") @DefaultValue("lifecycle") String context)
  {
    return webhookService.getAllWebhookEventTypes(context);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_WEBHOOK)
  public Webhook addWebhook(
      Webhook webhook,
      @QueryParam("context") String context)
  {
    return webhookService.addWebhook(webhook, context);
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_WEBHOOK)
  public Webhook updateWebhook(
      Webhook webhook,
      @QueryParam("context") @DefaultValue("lifecycle") String context)
  {
    return webhookService.updateWebhook(webhook, context);
  }

  @DELETE
  @Path(WEBHOOK_ID)
  @Audited(AuditEvent.DELETE_WEBHOOK)
  public void deleteWebhook(@PathParam("webhookId") final String webhookId) {
    webhookService.deleteWebhook(webhookId);
  }
}
