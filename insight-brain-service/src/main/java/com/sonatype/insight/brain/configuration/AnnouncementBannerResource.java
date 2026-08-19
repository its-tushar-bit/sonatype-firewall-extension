/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

/**
 * Read-only REST endpoint for the announcement banner. Readable by any authenticated user. The write path lives
 * on the MTIQ admin-only {@code @MtiqAdminEndpoint} resource.
 */
@Named
@Timed
@Path(AnnouncementBannerResource.RESOURCE_PATH)
public class AnnouncementBannerResource
{
  public static final String RESOURCE_PATH = "rest/config/announcementBanner";

  static final String FETCH_PATH = "fetch";

  private final AnnouncementBannerService service;

  @Inject
  public AnnouncementBannerResource(final AnnouncementBannerService service) {
    this.service = service;
  }

  @GET
  @Path(FETCH_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @UnlicensedPath
  public AnnouncementBanner getAnnouncementBanner() {
    return service.getBanner();
  }
}
