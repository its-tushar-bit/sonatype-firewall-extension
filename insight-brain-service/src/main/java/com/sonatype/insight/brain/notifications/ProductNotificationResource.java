/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.notifications;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.notifications.dto.ProductNotificationDTO;
import com.sonatype.insight.brain.notifications.dto.ProductNotificationListDTO;
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
