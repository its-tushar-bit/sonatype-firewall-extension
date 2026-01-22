/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiSpdxService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @since 1.164.0
 */
@Named
@Timed
@Singleton
@Path(PublicApiPaths.SPDX_RESOURCE_PATH)
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
@Tag(name = "SPDX",
    description = "Use this REST API to generate SPDX SBOMs in XML or JSON formats.")
public class ApiSpdxResource
{
  static final String GET_BY_STAGE_PATH = "{applicationId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "{applicationId}/reports/{scanId}";

  static final String DEFAULT_SPDX_FORMAT = "json";

  static final String DEFAULT_SPDX_VERSION = "2.3";

  private final ApiSpdxService apiSpdxService;

  @Inject
  public ApiSpdxResource(ApiSpdxService apiSpdxService) {
    this.apiSpdxService = apiSpdxService;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(description = "Use this method to generate SBOM(s) based on the latest application evaluation " +
      "report at the specified stage." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elements",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The requested SBOM(s).",
              content = {
                  @Content(
                      mediaType = MediaType.APPLICATION_XML,
                      schema = @Schema(type = "string", description = "SBOM in XML format")
                  ),
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(type = "string", description = "SBOM in JSON format")
                  ),
                  @Content(
                      mediaType = MediaType.APPLICATION_OCTET_STREAM,
                      schema = @Schema(type = "string", format = "binary", description = "SBOM archive (tar.gz)")
                  )
              }
          )
      }
  )
  public Response getLatestForStage(
      @Parameter(description = "Enter the applicationId for the application you want to generate the SBOM(s).",
          required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "Specify the stageId for the application evaluation. Allowed values are `develop`, " +
          "`build`, `stage-release`, `release` and `operate`.")
      @PathParam("stageId") String stageId,
      @Parameter(description = "Enter the format for the SBOM(s) to be generated.")
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @Parameter(description = "Set to `true` to generate an equivalent CycloneDx SBOM. Both SBOMs will be combined " +
          "as a tar.gz archive.")
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @Parameter(description = "Enter the desired SPDX version, possible values are 2.2|2.3")
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getLatestForStage(applicationId, stageId, format, generateCycloneDx, spdxVersion);
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(description = "Use this method to generate SBOM(s) based on a specific application scan." +
      "\n" +
      "\n" +
      "Permissions required: View IQ Elemets",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "The requested SBOM(s).",
              content = {
                  @Content(
                      mediaType = MediaType.APPLICATION_XML,
                      schema = @Schema(type = "string", description = "SBOM in XML format")
                  ),
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON,
                      schema = @Schema(type = "string", description = "SBOM in JSON format")
                  ),
                  @Content(
                      mediaType = MediaType.APPLICATION_OCTET_STREAM,
                      schema = @Schema(type = "string", format = "binary", description = "SBOM archive (tar.gz)")
                  )
              }
          )
      })
  public Response getByScanId(
      @Parameter(description = "Enter the applicationId for the application you want to generate the SBOM(s).",
          required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the scanId of the application scan.",
          required = true)
      @PathParam("scanId") String scanId,
      @Parameter(description = "Enter the format for the SBOM(s) to be generated.")
      @DefaultValue(DEFAULT_SPDX_FORMAT) @QueryParam("format") String format,
      @Parameter(description = "Set to `true` to generate an equivalent CycloneDx SBOM. Both SBOMs will be combined " +
          "as a tar.gz archive.")
      @DefaultValue("false") @QueryParam("generateCycloneDx") boolean generateCycloneDx,
      @Parameter(description = "Enter the desired SPDX version, possible values are 2.2|2.3")
      @DefaultValue(DEFAULT_SPDX_VERSION) @QueryParam("spdxVersion") String spdxVersion)
  {
    return apiSpdxService.getByScanId(applicationId, scanId, format, generateCycloneDx, spdxVersion);
  }
}
