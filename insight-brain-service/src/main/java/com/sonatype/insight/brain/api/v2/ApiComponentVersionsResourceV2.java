/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentOrPurlIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * @since 1.47
 */
@Named
@Timed
@Path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2)
public class ApiComponentVersionsResourceV2
{
  private final ApiComponentVersionsServiceV2 componentVersionsService;

  @Inject
  public ApiComponentVersionsResourceV2(final ApiComponentVersionsServiceV2 componentVersionsService) {
    this.componentVersionsService = componentVersionsService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve all known versions of a component.")
  @ApiResponse(responseCode = "200",
      description = "Known versions of the component are returned in a string array of ascending order.",
      useReturnTypeSchema = true)

  public List<String> getComponentVersions(
      @Parameter(description = "Possible values: Component identifier or packageURL (pURL) identifier in the " +
          "correct format. Use a-name for JavaScript components.") final ApiComponentOrPurlIdentifierDTOV2 componentOrPurlIdentifier)
  {
    return componentVersionsService.getComponentVersions(componentOrPurlIdentifier);
  }
}
