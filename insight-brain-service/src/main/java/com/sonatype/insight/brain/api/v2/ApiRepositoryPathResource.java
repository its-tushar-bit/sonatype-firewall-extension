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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryPathResponseDTO;
import com.sonatype.insight.brain.api.v2.service.ApiRepositoryPathService;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * At the time of this writing, there is no public API documentation for this.  The main purpose of this public api
 * endpoint is to be used by NXRM npm audit.  This could be publicly documented at a later date, if there was customer
 * interest and the endpoint isn't going to be abused.
 *
 * @since 1.125
 */
@Named
@Timed
@Path(PublicApiPaths.REPOSITORIES_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.FIREWALL)
public class ApiRepositoryPathResource
{
  static final String PATHNAMES_PATH =
      "{repositoryManagerInstanceId}/{repositoryPublicId}/components/quarantined/pathnames";

  private final ApiRepositoryPathService repositoryPathService;

  @Inject
  public ApiRepositoryPathResource(final ApiRepositoryPathService repositoryPathService) {
    this.repositoryPathService = repositoryPathService;
  }

  /**
   * @since 1.125
   */
  @POST
  @Path(PATHNAMES_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(description = "Use this method to retrieve the pathnames of the repository components and the " +
      "corresponding quarantine status." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The response contains a list of repository component paths and their quarantine status.",
              useReturnTypeSchema = true
          )
      })
  public ApiRepositoryPathResponseDTO getQuarantinedByPath(
      @Parameter(description = "Enter the repository manager instance ID.")
      @PathParam("repositoryManagerInstanceId") String repositoryManagerInstanceId,
      @Parameter(description = "Enter the repository public ID.")
      @PathParam("repositoryPublicId") String repositoryPublicId,
      @RequestBody(description = "Specify the pathnames.", required = true)
      List<String> pathnames)
  {
    return repositoryPathService.getQuarantinedByPathnames(repositoryManagerInstanceId, repositoryPublicId, pathnames);
  }
}
