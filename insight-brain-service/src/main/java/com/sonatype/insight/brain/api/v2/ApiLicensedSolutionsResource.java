/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLicensedSolutionDTO;
import com.sonatype.insight.brain.api.v2.service.ApiLicensedSolutionService;

import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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
  @Operation(
      description = "Retrieves a list of licensed solutions. "
          + "The base URL must be set to get results unless relative URLs are allowed." +
          "\n" +
          "\n" +
          "Permissions required: None ",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Successfully retrieved the list of licensed solutions.",
              useReturnTypeSchema = true
          )
      }
  )
  public List<ApiLicensedSolutionDTO> getLicensedSolutions(
      @Parameter(description = "Whether or not relative URLs should be allowed.")
      @DefaultValue("false") @QueryParam("allowRelativeUrls") boolean allowRelativeUrls)
  {
    return licensedSolutionService.getLicensedSolutions(allowRelativeUrls);
  }
}
