/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.UnlicensedPath;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

@Named
@Path(LandingResource.SERVICE_PATH)
@UnlicensedPath
public class LandingResource
{
  public static final String SERVICE_PATH = "";

  private final BaseUrl baseUrl;

  private CLMLicenseManager licenseManager;

  @Inject
  public LandingResource(CLMLicenseManager licenseManager, BaseUrl baseUrl) {
    this.licenseManager = licenseManager;
    this.baseUrl = baseUrl;
  }

  @GET
  public Response home() {
    UriBuilder uriBuilder = baseUrl.redirect();
    if (licenseManager.isValid()) {
      uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH.substring(1) + "reports.html");
    }
    else {
      uriBuilder.path(InsightBrainService.BRAIN_ASSET_PATH.substring(1) + "index.html");
    }
    return Response.seeOther(uriBuilder.build()).build();
  }
}