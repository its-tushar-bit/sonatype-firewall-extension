/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
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
  @Audited(AuditEvent.CONFIGURE_SYSTEM_NOTICE)
  public SystemNotice updateSystemNotice(SystemNotice systemNotice) {
    return systemNoticeService.updateSystemNotice(systemNotice);
  }
}
