/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.organization;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.v2.dto.ApplicationTotalRiskDTO;

import com.codahale.metrics.annotation.Timed;

@Named
@Timed
@Path(ApplicationSourceControlResource.RESOURCE_PATH)
public class ApplicationSourceControlResource
{
  static final String RESOURCE_PATH = "rest/sourceControl/application";

  private final ApplicationSourceControlService applicationSourceControlService;

  @Inject
  public ApplicationSourceControlResource(final ApplicationSourceControlService applicationSourceControlService) {
    this.applicationSourceControlService = applicationSourceControlService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApplicationTotalRiskDTO> getApplicationsWithAutomatedSourceControlFeedbackDisabled(
      @QueryParam("limit") final int limit)
  {
    return applicationSourceControlService.getApplicationsWithAutomatedSourceControlFeedbackDisabled(limit);
  }
}
