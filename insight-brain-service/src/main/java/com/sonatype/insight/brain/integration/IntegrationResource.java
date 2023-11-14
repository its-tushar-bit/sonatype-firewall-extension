/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.api.v2.dto.ApiIntegrationsCiCdStatIncrementDto;
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

  static final String GET_CICD_USAGE_STAT_INCREMENTS_OVERTIME_PATH = "/stats/cicd/usage-over-time";

  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  static final long FIVE_YEARS_IN_MS = 157_784_630_000L;

  private final IntegrationService integrationService;

  private final CIEvaluationStatService ciEvaluationStatService;

  @Inject
  public IntegrationResource(
      final IntegrationService integrationService,
      final CIEvaluationStatService ciEvaluationStatService
  )
  {
    this.integrationService = integrationService;
    this.ciEvaluationStatService = ciEvaluationStatService;
  }

  @GET
  @Path(STATUSES_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApplicationsWithIntegrationStatuses(
      @Context final UriInfo uriInfo,
      @DefaultValue(DEFAULT_PAGE) @QueryParam("page") final int page,
      @DefaultValue(DEFAULT_PAGE_SIZE) @QueryParam("pageSize") final int pageSize,
      @QueryParam("optionalOrderBy") final String optionalOrderBy,
      @QueryParam("optionalFilterApplicationNamesBy") final String optionalFilterApplicationNamesBy,
      @QueryParam("optionalFilterScmIsIntegrated") final Boolean optionalFilterAppsByScmIntegration,
      @QueryParam("optionalFilterCiCdIsIntegrated") final Boolean optionalFilterAppsByCiCdIntegration
  )
  {
    final ApiPageResult<IntegrationStatusDTO> results =
        integrationService.getIntegrationStatuses(page, pageSize, optionalOrderBy,
            optionalFilterApplicationNamesBy, optionalFilterAppsByScmIntegration, optionalFilterAppsByCiCdIntegration);
    return new PaginationResponseBuilder<>(uriInfo.getAbsolutePath().getPath(), page, pageSize, results)
        .queryParameters(uriInfo.getQueryParameters())
        .build();
  }

  @GET
  @Path(GET_CICD_USAGE_STAT_INCREMENTS_OVERTIME_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiIntegrationsCiCdStatIncrementDto> getCiCdUsageStatIncrementsOverTime(
      // one week default, maximum internal size of 5 years
      @DefaultValue("604800000")
      @Min(1) @Max(FIVE_YEARS_IN_MS)
      @QueryParam("incrementSizeMillis")
      long incrementSizeMillis,
      // 3 months default
      @DefaultValue("12") @Min(1) @Max(52) @QueryParam("numberOfIncrements") int numberOfIncrements
  )
  {
    return ciEvaluationStatService.getCiCdUsageStatsOverTime(incrementSizeMillis, numberOfIncrements);
  }
}
