/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerRequestDto;
import com.sonatype.insight.brain.api.v2.dto.RepositoryResultsForImageContainerResponseDto;
import com.sonatype.insight.brain.model.OwnerType;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Named
@Timed
@Path(PublicApiPaths.REPOSITORY_RESULTS_FOR_IMAGE_CONTAINER_PATH)
@Tag(name = "Repository Results for Image Container API",
    description = "This API provides access to the results of the image container scan")
public class ApiRepositoryResultsForImageContainerResource
{
  static  final String IMAGE_CONTAINER_PATH = "/{ownerId}/results/image-details";

  private final ApiRepositoryResultsForImageContainerService repositoryResultsService;

  @Inject
  public ApiRepositoryResultsForImageContainerResource(
      final ApiRepositoryResultsForImageContainerService repositoryResultsService)
  {
    this.repositoryResultsService = repositoryResultsService;
  }

  @POST
  @Path(IMAGE_CONTAINER_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Hidden
  @Operation(description = "This API provides access to the results of the image container scan",
      responses =
          {
              @ApiResponse
                  (responseCode = "200",
                      description = "The results of the image container scan"),
          }
  )
  public RepositoryResultsForImageContainerResponseDto getDetails(
      @Parameter(description = "Enter the value for ownerType.", required = true)
      @PathParam("ownerType") final OwnerType ownerType,
      @Parameter(description = "The public ID of the repository to get results for",
          required = true)
      @PathParam("ownerId") final String ownerId,
      @RequestBody(description = "The request JSON should contain" +
          "<ol>" +
          "<li>page (required) page number</li>" +
          "<li>pageSize (required)</li>" +
          "<li>threatLevelFilters (required) 0-10 range by default</li>" +
          "<li>violationStateFilters (required) empty if no filter set</li>" +
          "<li>searchFilters (required) empty if nothing to search" +
          "<li>sortFields (required)</li>" +
          "<li>aggregate (not required) result is returned non aggregated if aggregate is null</li>" +
          "</ol>")
      final RepositoryResultsForImageContainerRequestDto detailsRequest)
  {
    return repositoryResultsService.getDetails(ownerType, ownerId, detailsRequest);
  }
}
