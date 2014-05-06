/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dashboard.filters.PolicyThreatCategoryFilter;
import com.sonatype.insight.brain.dashboard.filters.PolicyThreatLevelFilter;

@Named
@Path(DashboardResource.SERVICE_PATH)
public class DashboardResource
{
  public static final String SERVICE_PATH = "rest/dashboard";

  public static final String GET_POLICY_VIOLATIONS_PATH = "policy/violations";

  public static final String GET_COMPONENT_RISKS_PATH = "policy/componentRisks";
  
  private DashboardService dashboardService;

  @Inject
  public DashboardResource(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Path(GET_POLICY_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<PolicyViolationDTO> getPolicyViolations(
      @QueryParam("applicationPublicIds") Set<String> applicationPublicIds,
      @QueryParam("stageIds") Set<String> stageIds,
      @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter,
      @QueryParam("maxResults") @DefaultValue("1000") int maxResults,
      @QueryParam("newest") @DefaultValue("false") boolean newest)
  {
    return dashboardService.getPolicyViolations(applicationPublicIds, stageIds, tagIds, policyThreatCategoryFilter, policyThreatLevelFilter, maxResults, newest);
  }

  @GET
  @Path(GET_COMPONENT_RISKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<ComponentRiskDTO> getComponentRisks(@QueryParam("applicationPublicIds") Set<String> applicationPublicIds,
      @QueryParam("stageIds") Set<String> stageIds, @QueryParam("tagIds") Set<String> tagIds,
      @QueryParam("policyThreatCategories") PolicyThreatCategoryFilter policyThreatCategoryFilter,
      @QueryParam("policyThreatLevelRange") PolicyThreatLevelFilter policyThreatLevelFilter,
      @QueryParam("maxResults") @DefaultValue("1000") int maxResults)
  {
    return dashboardService.getComponentRisks(applicationPublicIds, stageIds, tagIds, policyThreatCategoryFilter,
        policyThreatLevelFilter, maxResults);
  }
}
