/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiType;
import com.sonatype.insight.brain.api.v2.service.ApiEndpointsService;
import com.sonatype.insight.brain.product.license.UnlicensedPath;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.143.0
 */
@Named
@Timed
@Path(PublicApiPaths.ENDPOINTS_RESOURCE_PATH)
public class DefaultApiEndpointsResource
    implements ApiEndpointsResource
{
  private final ApiEndpointsService apiEndpointsService;

  @Inject
  public DefaultApiEndpointsResource(ApiEndpointsService apiEndpointsService) {
    this.apiEndpointsService = apiEndpointsService;
  }

  @Override
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @UnlicensedPath
  public String getOpenAPI(@Context Application application, @PathParam("apiType") ApiType apiType) {
    return apiEndpointsService.getOpenAPI(application, apiType);
  }
}
