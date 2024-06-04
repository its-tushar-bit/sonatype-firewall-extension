/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiLicensedSolutionService;

import io.micrometer.core.annotation.Timed;

@Named
@Timed
@Path(PublicApiPaths.LICENSED_SOLUTIONS_RESOURCE_PATH)
public class ApiLicensedSolutionsResource
{
  private final ApiLicensedSolutionService licensedSolutionService;

  @Inject
  public ApiLicensedSolutionsResource(ApiLicensedSolutionService licensedSolutionService) {
    this.licensedSolutionService = licensedSolutionService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<ApiLicensedSolutionDTO> getLicensedSolutions(
      @DefaultValue("false") @QueryParam("allowRelativeUrls") boolean allowRelativeUrls)
  {
    return licensedSolutionService.getLicensedSolutions(allowRelativeUrls);
  }
}
