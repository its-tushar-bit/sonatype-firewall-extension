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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.SpdxMediaType;
import com.sonatype.insight.brain.api.v2.dto.securesharing.ApiSecureSharingApplicationListDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSecureSharingService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.cyclonedx.CycloneDxMediaType;

@Named
@Timed
@Path(PublicApiPaths.DISTRIBUTE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
@HasFeature(SystemConfigurationPropertyFeature.SECURE_SHARING)
@Hidden
public class ApiSecureSharingResourceV2
{
  public static final String APPLICATIONS_PATH = "applications";

  private static final String APPLICATION_PATH = APPLICATIONS_PATH + "/{applicationIdOrPublicId}";

  private static final String SBOMS_PATH = APPLICATION_PATH + "/sboms";

  public static final String SBOM_VERSION_PATH = SBOMS_PATH + "/{sbomVersion}";

  private final ApiSecureSharingService apiSecureSharingService;

  @Inject
  public ApiSecureSharingResourceV2(final ApiSecureSharingService apiSecureSharingService) {
    this.apiSecureSharingService = apiSecureSharingService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path(APPLICATIONS_PATH)
  @Operation(
      summary = "Gets applications the user can export/import SBOMs from/to.",
      description = "Gets a paginated list of applications the user has" +
          " export SBOMs permission, import SBOMs permission, or both permissions on.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "A page of applications the user can export/import SBOMs from/to.",
              useReturnTypeSchema = true
          )
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

  @GET
  @Produces({
      CycloneDxMediaType.APPLICATION_CYCLONEDX_JSON,
      CycloneDxMediaType.APPLICATION_CYCLONEDX_XML,
      SpdxMediaType.APPLICATION_SPDX_JSON,
      SpdxMediaType.APPLICATION_SPDX_XML
  })
  @Path(SBOM_VERSION_PATH)
  @Audited(AuditEvent.EXPORT_SBOM_VERSION)
  @Operation(
      summary = "Exports an SBOM.",
      description = "Exports the requested SBOM version from the requested application" +
          " in the latest version for the requested format." +
          "<p>" +
          "Permissions Required: Export sboms.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "The requested SBOM content.",
              content = {
                  @Content(mediaType = CycloneDxMediaType.APPLICATION_CYCLONEDX_JSON),
                  @Content(mediaType = CycloneDxMediaType.APPLICATION_CYCLONEDX_XML),
                  @Content(mediaType = SpdxMediaType.APPLICATION_SPDX_JSON),
                  @Content(mediaType = SpdxMediaType.APPLICATION_SPDX_XML)
              }
          ),
          @ApiResponse(
              responseCode = "404",
              description = "The requested SBOM or its application are not found."
          )
      })
  public Response exportSbom(
      @Parameter(description = "The application ID or public ID.", required = true)
      @PathParam("applicationIdOrPublicId") final String applicationIdOrPublicId,
      @Parameter(description = "The SBOM version.", required = true) @PathParam("sbomVersion") final String sbomVersion,
      @Parameter(hidden = true) @HeaderParam(HttpHeaders.ACCEPT) final String accept)
  {
    return apiSecureSharingService.exportSbom(applicationIdOrPublicId, sbomVersion, accept);
  }
}
