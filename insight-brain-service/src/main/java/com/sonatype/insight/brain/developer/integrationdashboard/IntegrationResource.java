/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsCiCdStatIncrementDto;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiIntegrationsScmFeedbackStatIncrementDto;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.PaginationResponseBuilder;
import com.sonatype.insight.brain.developer.integrationdashboard.api.ApiUsageIncrementDto;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(IntegrationResource.RESOURCE_PATH)
public class IntegrationResource
{
  static final String RESOURCE_PATH = "/rest/integrations";

  static final String STATUSES_PATH = "/statuses";

  static final String GET_CICD_USAGE_STAT_INCREMENTS_OVERTIME_PATH = "/stats/cicd/usage-over-time";

  static final String GET_SCM_FEEDBACK_USAGE_STAT_INCREMENTS_OVER_TIME_PATH = "/stats/scm-feedback/usage-over-time";

  static final String GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH = "/stats/usage-over-time";

  static final String DEFAULT_ONE_WEEK_IN_MS = "604800000";

  static final String DEFAULT_NUMBER_OF_INCREMENTS = "12";

  static final String DEFAULT_PAGE = "1";

  static final String DEFAULT_PAGE_SIZE = "10";

  static final long FIVE_YEARS_IN_MS = 157_784_630_000L;

  private final IntegrationService integrationService;

  private final CIEvaluationStatService ciEvaluationStatService;

  private final ScmStatService scmStatService;

  private final ApplicationCountHistoryService applicationCountHistoryService;

  @Inject
  public IntegrationResource(
      final IntegrationService integrationService,
      final CIEvaluationStatService ciEvaluationStatService,
      final ScmStatService scmStatService,
      final ApplicationCountHistoryService applicationCountHistoryService
  )
  {
    this.integrationService = integrationService;
    this.ciEvaluationStatService = ciEvaluationStatService;
    this.scmStatService = scmStatService;
    this.applicationCountHistoryService = applicationCountHistoryService;
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
      @DefaultValue(DEFAULT_ONE_WEEK_IN_MS)
      @Min(1)
      @Max(FIVE_YEARS_IN_MS)
      @QueryParam("incrementSizeMillis")
      long incrementSizeMillis,
      // 3 months default
      @DefaultValue(DEFAULT_NUMBER_OF_INCREMENTS)
      @Min(1)
      @Max(52)
      @QueryParam("numberOfIncrements") int numberOfIncrements
  )
  {
    return ciEvaluationStatService.getCiCdUsageStatsOverTime(incrementSizeMillis, numberOfIncrements);
  }

  @GET
  @Path(GET_SCM_FEEDBACK_USAGE_STAT_INCREMENTS_OVER_TIME_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiIntegrationsScmFeedbackStatIncrementDto> getScmFeedbackUsageStatIncrementsOverTime(
      // one week default
      @DefaultValue(DEFAULT_ONE_WEEK_IN_MS)
      @Min(1)
      @QueryParam("incrementSizeMillis") long incrementSizeMillis,

      // 3 months default
      @DefaultValue(DEFAULT_NUMBER_OF_INCREMENTS)
      @Min(1)
      @Max(52)
      @QueryParam("numberOfIncrements") int numberOfIncrements
  )
  {
    return scmStatService.getScmFeedbackUsageStatsOverTime(incrementSizeMillis, numberOfIncrements);
  }

  @GET
  @Path(GET_APPLICATION_COUNT_HISTORY_OVER_TIME_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiUsageIncrementDto> getApplicationCountHistoryOverTime(
      // one week default
      @DefaultValue(DEFAULT_ONE_WEEK_IN_MS)
      @Min(1)
      @QueryParam("incrementSizeMillis") long incrementSizeMillis,

      // 3 months default
      @DefaultValue(DEFAULT_NUMBER_OF_INCREMENTS)
      @Min(1)
      @Max(52)
      @QueryParam("numberOfIncrements") int numberOfIncrements
  )
  {
    return applicationCountHistoryService.getUsageOverTime(incrementSizeMillis, numberOfIncrements);
  }
}
