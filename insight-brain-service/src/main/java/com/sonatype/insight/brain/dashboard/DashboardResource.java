/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Named
@Path(DashboardResource.SERVICE_PATH)
public class DashboardResource
{
  public static final String SERVICE_PATH = "rest/dashboard";

  public static final String GET_POLICY_VIOLATIONS_PATH = "policy/violations";
  
  private DashboardService dashboardService;

  @Inject
  public DashboardResource(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GET
  @Path(GET_POLICY_VIOLATIONS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public List<PolicyViolationDTO> getPolicyViolations(
      @QueryParam("applicationPublicIds") List<String> applicationPublicIds, @QueryParam("stageId") String stageId)
  {
    if (applicationPublicIds == null || applicationPublicIds.isEmpty()) {
      return dashboardService.getPolicyViolations(stageId);
    }

    return dashboardService.getPolicyViolationsByApplicationIds(applicationPublicIds, stageId);
  }
}
