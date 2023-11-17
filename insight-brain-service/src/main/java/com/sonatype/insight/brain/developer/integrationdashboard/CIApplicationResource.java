/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.dataaccess.CIApplicationFilter;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(CIApplicationResource.RESOURCE_PATH)
public class CIApplicationResource
{
  static final String RESOURCE_PATH = "rest/plugin/apps/ci";

  private final ApplicationRiskService applicationRiskService;

  @Inject
  public CIApplicationResource(final ApplicationRiskService applicationRiskService) {
    this.applicationRiskService = applicationRiskService;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public DashboardResultsDTO<ApplicationTotalRiskDTO> getCIApplicationRisks(final CIApplicationFilter filter) {
    return applicationRiskService.getCIApplicationRisk(filter);
  }
}
