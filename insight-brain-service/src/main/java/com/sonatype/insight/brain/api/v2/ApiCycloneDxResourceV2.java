/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.scan.file.ThirdPartyUtils;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.cyclonedx.Version;

/**
 * @since 1.70
 */
@Named
@Timed
@Singleton
@Path(PublicApiPaths.CYCLONE_DX_RESOURCE_PATH)
@Tag(name = "CycloneDX",
    description = "Use the CycloneDX REST API to generate CycloneDX SBOMs in XML or JSON formats, containing " +
        "coordinates and licenses for components found in a scan report.")
@ProductLicenseEnforcementPoint(LicensedFeature.SBOM_REPORTS)
public class ApiCycloneDxResourceV2
{
  static final String GET_BY_STAGE_PATH = "{applicationId}/stages/{stageId}";

  static final String GET_BY_STAGE_PATH_WITH_VERSION =
      "{cdxVersion: 1.1|1.2|1.3|1.4|1.5|1.6}/{applicationId}/stages/{stageId}";

  static final String GET_BY_REPORT_PATH = "{applicationId}/reports/{reportId}";

  /**
   * When adding a new version or changing this path, please update
   * {@link UserInterfaceLinksResource#linkToSbom(String, String)} as well.
   */
  static final String GET_BY_REPORT_PATH_WITH_VERSION =
      "{cdxVersion: 1.1|1.2|1.3|1.4|1.5|1.6}/{applicationId}/reports/{reportId}";

  private final ApiCycloneDxServiceV2 apiCycloneDxService;

  @Inject
  public ApiCycloneDxResourceV2(ApiCycloneDxServiceV2 apiCycloneDxService) {
    this.apiCycloneDxService = apiCycloneDxService;
  }

  @GET
  @Path(GET_BY_STAGE_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Deprecated
  @Operation(
      hidden = true,
      description = "Use this method to generate a CycloneDX SBOM for an application." +
          "<p>" +
          "Permissions Required: View IQ Elements")
  @ApiResponse(responseCode = "200", description = "A downloadable file will be generated.",
      content = @Content(
          mediaType = "application/xml"))
  public Response getLatest(
      @Parameter(description = "Enter the internal applicationId for the application you want to generate the SBOM. " +
          "You can also retrieve it using the Application REST API",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(
          description = "Enter the stageId to generate the SBOM based on the latest application policy evaluation at that stage. "
              +
              "Allowed values for stageId are 'develop', 'source', 'build', 'stage-release', 'release', and, " +
              "'operate'.") @PathParam("stageId") String stageId)
  {
    return apiCycloneDxService.getLatest(applicationId, stageId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_STAGE_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(
      description = "Use this method to generate a CycloneDX SBOM for an application." +
          "<p>" +
          "Permissions Required: View IQ Elements")
  @ApiResponse(responseCode = "200", description = "A downloadable file will be generated.",
      content = {
        @Content(mediaType = "application/json"),
        @Content(mediaType = "application/xml")
      })
  public Response getLatest(
      @Parameter(description = "Enter the internal applicationId for the application you want to generate the SBOM. " +
          "You can also retrieve the applicationId using the Application REST API.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(
          description = "Enter the stageId to generate the SBOM based on the latest application policy evaluation at that stage. "
              +
              "Allowed values for stageId are 'develop', 'source', 'build', 'stage-release', 'release', and, " +
              "'operate'.") @PathParam("stageId") String stageId,
      @Parameter(
          description = "Possible values are 1.1|1.2|1.3|1.4|1.5|1.6.") @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    String acceptType = determineAcceptableMediaType(headers);
    return apiCycloneDxService.getLatest(applicationId, stageId, acceptType,
        ThirdPartyUtils.getCycloneDxSchemaVersion(cycloneDxVersion));
  }

  @GET
  @Path(GET_BY_REPORT_PATH)
  @Produces(MediaType.APPLICATION_XML)
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Deprecated
  @Operation(
      hidden = true,
      description = "Use this method to generate a CycloneDX SBOM for an application." +
          "<p>" +
          "Permissions Required: View IQ Elements")
  @ApiResponse(responseCode = "200", description = "A downloadable file will be generated.",
      content = @Content(
          mediaType = "application/xml"))
  public Response getByReportId(
      @Parameter(description = "Enter the internal applicationId for the application you want to generate the SBOM. " +
          "You can also retrieve the applicationId using the Application REST API.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the reportId to generate the SBOM for the application for a specific " +
          "scan report.") @PathParam("reportId") String reportId)
  {
    return apiCycloneDxService
        .getByScanId(applicationId, reportId, MediaType.APPLICATION_XML, Version.VERSION_11);
  }

  @GET
  @Path(GET_BY_REPORT_PATH_WITH_VERSION)
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.EXPORT_APPLICATION_COMPOSITION_REPORT)
  @Operation(
      description = "Use this method to generate a CycloneDX SBOM for an application." +
          "<p>" +
          "Permissions Required: View IQ Elements"

  )
  @ApiResponse(responseCode = "200", description = "A downloadable file will be generated.",
      content = {
        @Content(mediaType = "application/json"),
        @Content(mediaType = "application/xml")
      })
  public Response getByReportId(
      @Parameter(description = "Enter the internal applicationId for the application you want to generate the SBOM. " +
          "You can also retrieve the applicationId using the Application REST API.",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "Enter the reportId to generate the SBOM for the application for a " +
          "specific scan report.") @PathParam("reportId") String reportId,
      @Parameter(
          description = "Possible values are 1.1|1.2|1.3|1.4|1.5|1.6.") @PathParam("cdxVersion") String cycloneDxVersion,
      @Context HttpHeaders headers)
  {
    String acceptType = determineAcceptableMediaType(headers);
    return apiCycloneDxService.getByScanId(applicationId, reportId, acceptType,
        ThirdPartyUtils.getCycloneDxSchemaVersion(cycloneDxVersion));
  }

  private String determineAcceptableMediaType(final HttpHeaders headers) {
    if (headers != null && CollectionUtils.isNotEmpty(headers.getAcceptableMediaTypes())) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(headers.getAcceptableMediaTypes().get(0))) {
        return MediaType.APPLICATION_JSON_TYPE.toString();
      }
    }
    return MediaType.APPLICATION_XML_TYPE.toString();
  }
}
