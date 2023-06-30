/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(ApplicationSourceControlResource.RESOURCE_PATH)
public class ApplicationSourceControlResource
{
  static final String RESOURCE_PATH = "rest/sourceControl/application";

  private final ApplicationRiskService applicationRiskService;

  @Inject
  public ApplicationSourceControlResource(final ApplicationRiskService applicationRiskService) {
    this.applicationRiskService = applicationRiskService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public DashboardResultsDTO<ApplicationTotalRiskDTO> getApplicationsWithAutomatedSourceControlFeedbackDisabled(
      @QueryParam("page") final int page, @QueryParam("pageSize") final int pageSize)
  {
    return applicationRiskService.getApplicationsWithAutomatedSourceControlFeedbackDisabledRisk(page, pageSize);
  }
}
