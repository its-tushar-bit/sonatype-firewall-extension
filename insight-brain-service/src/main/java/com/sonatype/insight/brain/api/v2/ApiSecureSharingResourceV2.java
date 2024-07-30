/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSecureSharingService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Named
@Timed
@Path(PublicApiPaths.DISTRIBUTE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
@HasFeature(SystemConfigurationPropertyFeature.SECURE_SHARING)
@Hidden
public class ApiSecureSharingResourceV2
{
  public static final String APPLICATIONS_PATH = "applications";

  private final ApiSecureSharingService apiSecureSharingService;

  @Inject
  public ApiSecureSharingResourceV2(final ApiSecureSharingService apiSecureSharingService) {
    this.apiSecureSharingService = apiSecureSharingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(APPLICATIONS_PATH)
  @Operation(summary = "Gets applications the user can export/import SBOMs from/to.",
      description = "Gets a paginated list of applications the user has" +
          " export SBOMs permission, import SBOMs permission, or both permissions on.",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "A page of applications the user can export/import SBOMs from/to.",
              useReturnTypeSchema = true)
      })
  public ApiSecureSharingApplicationListDTO getApplicationsWithPermissions(
      @Parameter(description = "A permission to filter on, either 'export' or 'import'.") @QueryParam("permission")
      final Set<String> permissions,
      @Parameter(description = "The page number.") @QueryParam("page") @DefaultValue("1") final int page,
      @Parameter(description = "The page size.") @QueryParam("pageSize") @DefaultValue("1000") final int pageSize)
  {
    return apiSecureSharingService.getApplicationsWithPermissions(
        ApiSecureSharingService.resolvePermissions(permissions), page, pageSize);
  }
}
