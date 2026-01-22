/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.product.notifications;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.14.0
 */
@Named
@Timed
@Path(ProductNotificationResource.RESOURCE_PATH)
@UnlicensedPath
public class ProductNotificationResource
{
  public static final String RESOURCE_PATH = "rest/product/notifications";

  public static final String VIEWED_PATH = "viewed";

  private final ProductNotificationService notificationsService;

  @Inject
  public ProductNotificationResource(final ProductNotificationService notificationsService) {
    this.notificationsService = notificationsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public ProductNotificationListDTO getNotifications(@QueryParam("pageSize") @DefaultValue("20") final int pagesSize,
                                                     @QueryParam("page") @DefaultValue("1") final int page)
  {
    return notificationsService.getNotifications(pagesSize, page);
  }

  @POST
  @Path(VIEWED_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public ProductNotificationDTO setNotificationViewed(final ProductNotificationDTO notificationDTO) {
    return notificationsService.setNotificationViewed(notificationDTO);
  }
}
