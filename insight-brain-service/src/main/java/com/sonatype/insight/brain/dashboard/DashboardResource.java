/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

import com.yammer.metrics.annotation.ExceptionMetered;
import com.yammer.metrics.annotation.Metered;
import com.yammer.metrics.annotation.Timed;

@Named
@Path(DashboardResource.SERVICE_PATH)
public class DashboardResource
{
  public static final String SERVICE_PATH = "rest/dashboard";

  public static final String GET_NEWEST_RISKS_PATH = "policy/newestRisks";

  public static final String GET_COMPONENT_RISKS_PATH = "policy/componentRisks";

  public static final String GET_APPLICATION_RISKS_PATH = "policy/applicationRisks";

  public static final String GET_POLICY_SUMMARY_PATH = "policy/summary";

  public static final String FILTERS_PATH = "filters";

  public static final String FILTERS_SUMMARY_PATH = "filters/summary";

  public static final String COMPONENTS_SUMMARY_PATH = "components/summary";

  private final ApplicationRiskService applicationRiskService;

  private final ComponentRiskService componentRiskService;

  private final ComponentSummaryService componentSummaryService;

  private final DashboardFilterService dashboardFilterService;

  private final NewestRiskService newestRiskService;

  private final PolicySummaryService policySummaryService;

  @Inject
  public DashboardResource(ApplicationRiskService applicationRiskService,
      DashboardFilterService dashboardFilterService, ComponentRiskService componentRiskService,
      ComponentSummaryService componentSummaryService, NewestRiskService newestRiskService,
      PolicySummaryService policySummaryService)
  {
    this.applicationRiskService = applicationRiskService;
    this.componentRiskService = componentRiskService;
    this.componentSummaryService = componentSummaryService;
    this.dashboardFilterService = dashboardFilterService;
    this.newestRiskService = newestRiskService;
    this.policySummaryService = policySummaryService;
  }

  @GET
  @Path(GET_NEWEST_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getNewestRisksMeter")
  @ExceptionMetered(name = "getNewestRisksExceptionMeter")
  public List<NewestRiskDTO> getNewestRisks(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter,
      @QueryParam("maxResults") @DefaultValue("1000") int maxResults)
  {
    return newestRiskService.getNewestRisks(applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, maxResults);
  }

  @GET
  @Path(GET_APPLICATION_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getApplicationRisksMeter")
  @ExceptionMetered(name = "getApplicationRisksExceptionMeter")
  public List<ApplicationRiskScoreDTO> getApplicationRisks(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds,
      @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter,
      @QueryParam("maxResults") @DefaultValue("1000") int maxResults)
  {
    return applicationRiskService.getApplicationRisks(applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, maxResults);
  }

  @GET
  @Path(GET_COMPONENT_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getComponentRisksMeter")
  @ExceptionMetered(name = "getComponentRisksExceptionMeter")
  public List<ComponentRiskDTO> getComponentRisks(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter,
      @QueryParam("maxResults") @DefaultValue("1000") int maxResults)
  {
    return componentRiskService.getComponentRisks(applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, maxResults);
  }

  /**
   * @since 1.11.0
   */
  @GET
  @Path(FILTERS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getFilterMeter")
  @ExceptionMetered(name = "getFilterExceptionMeter")
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
  @Metered(name = "updateFilterMeter")
  @ExceptionMetered(name = "updateFilterExceptionMeter")
  public DashboardFilterDTO updateUserDashboardFilterForCurrentUser(DashboardFilterDTO dashboardFilterDTO) {
    return dashboardFilterService.createOrUpdateDashboardFilterForCurrentUser(dashboardFilterDTO);
  }

  /**
   * @since 1.11.0
   */
  @DELETE
  @Path(FILTERS_PATH)
  @Timed
  @Metered(name = "deleteFilterMeter")
  @ExceptionMetered(name = "deleteFilterExceptionMeter")
  public void deleteDashboardFilterForCurrentUser() {
    dashboardFilterService.deleteDashboardFilterForCurrentUser();
  }

  @GET
  @Path(FILTERS_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getFilterSummaryMeter")
  @ExceptionMetered(name = "getFilterSummaryExceptionMeter")
  public FilterSummaryDTO getFilterSummary(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    return dashboardFilterService.getFilterSummary(applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter);
  }

  @GET
  @Path(COMPONENTS_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getComponentSummaryMeter")
  @ExceptionMetered(name = "getComponentSummaryExceptionMeter")
  public ComponentSummaryDTO getComponentSummary(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds)
  {
    return componentSummaryService.getComponentSummary(applicationIds, stageIds, tagIds);
  }

  @GET
  @Path(GET_POLICY_SUMMARY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Timed
  @Metered(name = "getPolicySummaryMeter")
  @ExceptionMetered(name = "getPolicySummaryExceptionMeter")
  public PolicySummaryDTO getPolicySummary(@QueryParam("applicationIds") Set<String> applicationIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter)
  {
    return policySummaryService.getPolicySummary(applicationIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter);
  }
}
