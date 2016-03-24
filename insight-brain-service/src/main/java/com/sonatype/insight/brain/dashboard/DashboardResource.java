/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Timed;

@Named
@Path(DashboardResource.RESOURCE_PATH)
public class DashboardResource
{
  public static final String RESOURCE_PATH = "rest/dashboard";

  public static final String GET_NEWEST_RISKS_PATH = "policy/newestRisks";

  public static final String GET_COMPONENT_RISKS_PATH = "policy/componentRisks";

  public static final String GET_APPLICATION_RISKS_PATH = "policy/applicationRisks";

  public static final String GET_POLICY_SUMMARY_PATH = "policy/summary";

  public static final String FILTERS_PATH = "filters";

  public static final String FILTERS_SUMMARY_PATH = "filters/summary";

  private final ApplicationRiskService applicationRiskService;

  private final ComponentRiskService componentRiskService;

  private final DashboardFilterService dashboardFilterService;

  private final NewestRiskService newestRiskService;

  private final PolicySummaryService policySummaryService;

  @Inject
  public DashboardResource(ApplicationRiskService applicationRiskService,
                           DashboardFilterService dashboardFilterService,
                           ComponentRiskService componentRiskService,
                           NewestRiskService newestRiskService,
                           PolicySummaryService policySummaryService)
  {
    this.applicationRiskService = applicationRiskService;
    this.componentRiskService = componentRiskService;
    this.dashboardFilterService = dashboardFilterService;
    this.newestRiskService = newestRiskService;
    this.policySummaryService = policySummaryService;
  }

  @POST
  @Path(GET_NEWEST_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getNewestRisksExceptionMeter")
  public List<NewestRiskDTO> getNewestRisks(RisksFilterDTO risksFilterDTO)
  {
    return newestRiskService
        .getNewestRisks(risksFilterDTO.applicationIds, risksFilterDTO.stageIds, risksFilterDTO.tagIds,
            risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  @POST
  @Path(GET_APPLICATION_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getApplicationRisksExceptionMeter")
  public List<ApplicationRiskScoreDTO> getApplicationRisks(RisksFilterDTO risksFilterDTO)
  {
    return applicationRiskService
        .getApplicationRisks(risksFilterDTO.applicationIds, risksFilterDTO.stageIds, risksFilterDTO.tagIds,
            risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  @POST
  @Path(GET_COMPONENT_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getComponentRisksExceptionMeter")
  public List<ComponentRiskDTO> getComponentRisks(RisksFilterDTO risksFilterDTO)
  {
    return componentRiskService
        .getComponentRisks(risksFilterDTO.applicationIds, risksFilterDTO.stageIds, risksFilterDTO.tagIds,
            risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange, risksFilterDTO.maxResults);
  }

  /**
   * @since 1.11.0
   */
  @GET
  @Path(FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getDashboardFilterForCurrentUserExceptionMeter")
  public DashboardFilterDTO getDashboardFilterForCurrentUser() throws IOException {
    return dashboardFilterService.getDashboardFilterForCurrentUser();
  }

  /**
   * @since 1.11.0
   */
  @PUT
  @Path(FILTERS_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "updateUserDashboardFilterForCurrentUserExceptionMeter")
  public DashboardFilterDTO updateUserDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }

  /**
   * @since 1.11.0
   */
  @DELETE
  @Path(FILTERS_PATH)
  @Timed
  @ExceptionMetered(name = "deleteDashboardFilterForCurrentUserExceptionMeter")
  public void deleteDashboardFilterForCurrentUser() {
    dashboardFilterService.deleteDashboardFilterForCurrentUser();
  }

  @POST
  @Path(FILTERS_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getFilterSummaryExceptionMeter")
  public FilterSummaryDTO getFilterSummary(RisksFilterDTO risksFilterDTO)
  {
    return dashboardFilterService
        .getFilterSummary(risksFilterDTO.applicationIds, risksFilterDTO.stageIds, risksFilterDTO.tagIds,
            risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange);
  }

  @POST
  @Path(GET_POLICY_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  @ExceptionMetered(name = "getPolicySummaryExceptionMeter")
  public PolicySummaryDTO getPolicySummary(RisksFilterDTO risksFilterDTO)
  {
    return policySummaryService
        .getPolicySummary(risksFilterDTO.applicationIds, risksFilterDTO.stageIds, risksFilterDTO.tagIds,
            risksFilterDTO.policyThreatCategories, risksFilterDTO.policyThreatLevelRange);
  }
}
