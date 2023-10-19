/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.api.v2.dto.IntegrationStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(IntegrationResource.RESOURCE_PATH)
public class IntegrationResource
{
  static final String RESOURCE_PATH = "/rest/integrations";

  static final String STATUSES_PATH = "/statuses";

  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  private final IntegrationService integrationService;

  @Inject
  public IntegrationResource(final IntegrationService integrationService) {
    this.integrationService = integrationService;
  }

  @GET
  @Path(STATUSES_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApplicationIntegrationStatuses(
      @Context final UriInfo uriInfo,
      @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
      @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
      @QueryParam("optionalOrderBy") final String optionalOrderBy,
      @QueryParam("optionalFilterApplicationNamesBy") final String optionalFilterApplicationNamesBy)
  {
    final ApiPageResult<IntegrationStatusDTO> results =
        integrationService.getIntegrationStatuses(page, pageSize, optionalOrderBy,
            optionalFilterApplicationNamesBy);
    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize, results)
        .queryParameters(uriInfo.getQueryParameters())
        .build();
  }
}
