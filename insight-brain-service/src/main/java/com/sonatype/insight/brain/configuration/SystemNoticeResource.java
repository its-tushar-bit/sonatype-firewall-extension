/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.29.0
 */
@Named
@Timed
@Path(SystemNoticeResource.RESOURCE_PATH)
public class SystemNoticeResource
{
  public static final String RESOURCE_PATH = "rest/config/systemNotice";

  static final String FETCH_PATH = "fetch";

  private SystemNoticeService systemNoticeService;

  @Inject
  public SystemNoticeResource(final SystemNoticeService systemNoticeService) {
    this.systemNoticeService = systemNoticeService;
  }

  @GET
  @Path(FETCH_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @UnlicensedPath
  public SystemNotice getSystemNotice() {
    return systemNoticeService.getSystemNotice();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public SystemNotice updateSystemNotice(SystemNotice systemNotice) {
    return systemNoticeService.updateSystemNotice(systemNotice);
  }
}
