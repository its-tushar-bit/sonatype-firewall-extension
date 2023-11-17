/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.CIEvaluationStatDTO;

import com.codahale.metrics.annotation.Timed;

/**
 * CI/CD plugin stat rest resource returning the percentage of applications with a CI/CD plugin-triggered evaluation
 * <p>
 * since 1.162
 */
@Named
@Timed
@Path(CIEvaluationStatResource.RESOURCE_PATH)
public class CIEvaluationStatResource
{
  static final String RESOURCE_PATH = "rest/plugin/stat/ci";

  private final CIEvaluationStatService ciEvaluationStatService;

  @Inject
  public CIEvaluationStatResource(CIEvaluationStatService ciEvaluationStatService) {
    this.ciEvaluationStatService = ciEvaluationStatService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public CIEvaluationStatDTO getDataForAppsWithoutCITriggeredEvaluations(
      @QueryParam("sinceUtcTimestamp") final long sinceUtcTimestamp)
  {
    return ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);
  }
}
