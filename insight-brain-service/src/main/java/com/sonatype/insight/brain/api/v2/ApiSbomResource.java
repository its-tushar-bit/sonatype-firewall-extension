/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.ComponentLocator;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomVulnerabilityAnalysisRequestDTO.VulnerabilityAnalysis;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.api.v2.dto.SecurityVulnerabilityDataDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.api.v2.service.ApiSbomVulnerabilityService;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomComponentSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomVersionsApplicationSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentListDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.SBOM_RESOURCE_PATH)
@Tag(name = "SBOM")
public class ApiSbomResource
{
  static final String DEFAULT_SBOM_STATE = "current";

  static final String DEFAULT_SBOM_SPECIFICATION = "cyclonedx1.6";

  public static final String SBOMS_APPLICATIONS_PATH = "/applications";

  static final String SBOMS_APPLICATION_PATH = SBOMS_APPLICATIONS_PATH + "/{applicationId}";

  static final String SBOM_VERSIONS_PATH = SBOMS_APPLICATION_PATH + "/versions";

  public static final String SBOM_VERSION_PATH = SBOM_VERSIONS_PATH + "/{version}";

  static final String SBOM_COMPONENTS_PATH = SBOM_VERSION_PATH + "/components";

  public static final String SBOM_IMPORT_PATH = "/import";

  public static final String SBOM_STATUS_PATH = SBOMS_APPLICATION_PATH + "/status/{importRequestId}";

  public static final String SBOM_VULNERABILITY_PATH = SBOM_VERSION_PATH + "/vulnerability/{refId}";

  public static final String SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH = SBOM_VULNERABILITY_PATH + "/analysis";

  private final ApiSbomService apiSbomService;

  private final ApiSbomVulnerabilityService apiSbomVulnerabilityService;

  @Inject
  public ApiSbomResource(
      final ApiSbomService apiSbomService,
      final ApiSbomVulnerabilityService apiSbomVulnerabilityService)
  {
    this.apiSbomService = apiSbomService;
    this.apiSbomVulnerabilityService = apiSbomVulnerabilityService;
  }

  @Operation(summary = "Delete sbom version",
      description = "Deletes a specific sbom version including it's original contents and updates",
      responses = {
        @ApiResponse(responseCode = "404", description = "Supplied sbom version not found"),
        @ApiResponse(responseCode = "204", description = "Delete successful")
      })
  @DELETE
  @Path(SBOM_VERSION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Audited(AuditEvent.DELETE_SBOM_VERSION)
  public void deleteSbomVersion(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "URL Encoded version value of the sbom to be deleted",
          required = true) @PathParam("version") String version) throws IOException
  {
    apiSbomService.deleteSbomVersion(applicationId, version);
  }

  @Operation(summary = "Gets a sbom version",
      description = "Downloads a specific sbom version in its original or current form",
      responses = {
        @ApiResponse(responseCode = "404", description = "Supplied sbom version not found"),
        @ApiResponse(responseCode = "200",
            description = "Content of the sbom",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(type = "string", description = "SBOM content in JSON format")),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(type = "string", description = "SBOM content in XML format"))
            })
      })
  @GET
  @Path(SBOM_VERSION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response getSbomVersion(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,

      @Parameter(description = "URL Encoded version value of the sbom",
          required = true) @PathParam("version") String version,

      @Parameter(
          description = "The state of the sbom version. Allowed values [original|current]. default = current") @DefaultValue(DEFAULT_SBOM_STATE) @QueryParam("state") String sbomState,

      @Parameter(description = "Target specification of the sbom. Allowed values " +
          "[cyclonedx1.6|cyclonedx1.5|spdx2.2|spdx2.3]. default = cyclonedx1.6") @DefaultValue(DEFAULT_SBOM_SPECIFICATION) @QueryParam("specification") String targetSpecification,

      @Parameter(in = ParameterIn.HEADER, name = "Accept", description = "Output format(json/xml) of the sbom. " +
          "Changing the output format only applicable when downloading the current form of the SBOM. " +
          "The original sbom will always return in the original form that it was ingested. " +
          "When requesting `current` form and if this header value is not present the sbom will be returned " +
          "in its original ingested format. " +
          "Allowed values {'application/json'|'application/xml'}. default = null") @HeaderParam(HttpHeaders.ACCEPT) String acceptMediaType)
  {
    return apiSbomService.getSbomVersion(applicationId, version, sbomState, targetSpecification, acceptMediaType);
  }

  @Operation(summary = "Gets a paginated list of SBOMs for an application",
      description = "Gets a paginated list of SBOMs for an application",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "list of the sboms",
            content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOMS_APPLICATION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  public ThirdPartySbomMetadataSummaryListDTO getSbomMetadataSummaryForApplication(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,

      @Deprecated @Parameter(description = "Deprecated, use sortBy and asc instead."
          + " Sort results by import date. Allowed values [asc|desc]. default = asc",
          deprecated = true) @DefaultValue("asc") @QueryParam("sortByDate") String sortByDate,

      @Parameter(
          description = "Number of items to return by page. default = 10") @DefaultValue("10") @QueryParam("pageSize") int pageSize,

      @Parameter(description = "Current page number. default = 1") @DefaultValue("1") @QueryParam("page") int page,

      @Parameter(
          description = "Criteria to sort the results. default = IMPORT_DATE, when used sortByDate is ignored") @DefaultValue("IMPORT_DATE") @QueryParam("sortBy") SbomVersionsApplicationSortableField sortBy,

      @Parameter(
          description = "Order mode ASC=true or DESC=false. default = true") @DefaultValue("true") @QueryParam("asc") boolean asc)
  {
    return apiSbomService.getSbomMetadataSummaryForApplication(applicationId, sortByDate, pageSize, page, sortBy, asc);
  }

  @Operation(summary = "Gets the components found in a specific sbom version",
      description = "Lists the components in a specific sbom version with data about vulnerabilities and licenses",
      responses = {
        @ApiResponse(responseCode = "200", description = "List of components in the sbom",
            content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOM_COMPONENTS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public SbomComponentListDTO getSbomComponents(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,

      @Parameter(description = "URL Encoded version value of the sbom to query its components",
          required = true) @PathParam("version") String version,

      @Parameter(
          description = "If provided, filter components by the given threat level on their vulnerabilities") @QueryParam("vulnerabilityThreatLevels") Set<CvssV3Severity> vulnerabilityThreatLevels,

      @Parameter(
          description = "If provided, filter components by the given dependency types") @QueryParam("dependencyTypes") Set<ThirdPartyDependencyType> dependencyTypes,

      @Parameter(
          description = "If provided, filter components by the given search criteria") @QueryParam("filter") String filterText,

      @Parameter(
          description = "Criteria to sort the results. default = VULNERABILITIES") @DefaultValue("VULNERABILITIES") @QueryParam("sortBy") SbomComponentSortableField sortBy,

      @Parameter(
          description = "Order mode ASC=true or DESC=false. default = false") @DefaultValue("false") @QueryParam("asc") boolean asc,

      @Parameter(description = "Current page number. default = 1") @DefaultValue("1") @QueryParam("page") int page,

      @Parameter(
          description = "Number of items to return by page. default = 50") @DefaultValue("50") @QueryParam("pageSize") int pageSize)
  {
    return apiSbomService.getSbomComponents(applicationId, version, vulnerabilityThreatLevels, dependencyTypes,
        filterText, sortBy, asc, pageSize, page);
  }

  @Operation(summary = "Gets a list of active sbom versions by application id",
      description = "Gets a list of active sbom versions by application id",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "list of the active sbom versions by application id",
            content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOM_VERSIONS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  public List<String> getActiveSbomVersionListByApplication(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId)
  {
    return apiSbomService.getActiveSbomVersionListByApplication(applicationId);
  }

  @Operation(summary = "Import a new sbom version",
      description = "Imports a new sbom version to an existing application",
      responses = {
        @ApiResponse(responseCode = "400", description = "Invalid/Unsupported data provided for sbom import"),
        @ApiResponse(responseCode = "202",
            description = "Import successful. URL to check the status of the import returned",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiThirdPartyScanTicketDTO.class)))
      })

  @POST
  @Path(SBOM_IMPORT_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Audited(AuditEvent.IMPORT_SBOM_VERSION)
  public Response importSbom(
      @Parameter(description = "The internal id of the application.",
          required = true) @FormDataParam("applicationId") String applicationId,
      @Parameter(required = true,
          schema = @Schema(type = "string", format = "binary",
              description = "Your SBOM.")) @FormDataParam("file") InputStream inputStream,
      @Parameter(hidden = true) @FormDataParam("file") FormDataContentDisposition fileDetail,
      @Parameter(description = "The SBOM version.") @FormDataParam("applicationVersion") String applicationVersion,
      @Parameter(
          description = "Enable importing as a binary file.") @QueryParam("enableBinaryImport") @DefaultValue("false") boolean enableBinaryImport,
      @Parameter(
          description = "Skip the SBOM validation if an error occurs.") @QueryParam("ignoreValidationError") @DefaultValue("false") boolean ignoreValidationError,
      @Context final HttpServletRequest request)
  {
    if (StringUtils.isBlank(applicationId)) {
      throw new BadRequestException("Missing required parameter [applicationId]");
    }
    return apiSbomService.importSbom(applicationId, inputStream, fileDetail.getFileName(), enableBinaryImport,
        HdsClient.getClientUserAgent(request), applicationVersion, ignoreValidationError);
  }

  @Operation(summary = "Get sbom import status",
      description = "Gets status of a sbom import.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Sbom import still in progress."),
        @ApiResponse(responseCode = "200",
            description = "Sbom import completed successfully.",
            content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOM_STATUS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  public ApiSbomStatusDTO getImportStatus(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "The id of the import request",
          required = true) @PathParam("importRequestId") String importRequestId)
  {
    return apiSbomService.getImportStatus(applicationId, importRequestId);
  }

  @Operation(
      description = "Use this method to retrieve details for a vulnerability belongs to a specific sbom version ",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "The response contains vulnerability details corresponding to the vulnerability",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Target vulnerability not found")
      })

  @GET
  @Path(SBOM_VULNERABILITY_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  public SecurityVulnerabilityDataDTO getVulnerabilityDetails(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "The version for a specific SBOM where the vulnerability " +
          "is present", required = true) @PathParam("version") String sbomVersion,
      @Parameter(description = "The vulnerability id of a vulnerability",
          required = true) @PathParam("refId") String refId,
      @Parameter(description = "(One of packageUrl or componentHash is required) Enter the packageUrl " +
          "of the component with the vulnerability") @QueryParam("packageUrl") String packageUrl,
      @Parameter(description = "(One of packageUrl or componentHash is required) Enter the componentHash " +
          "of the component with the vulnerability") @QueryParam("componentHash") String componentHash)
  {
    return apiSbomVulnerabilityService.getSecurityVulnerabilityDetailsDTO(applicationId, sbomVersion, refId,
        packageUrl, componentHash);
  }

  @Operation(summary = "Updates a vulnerability analysis annotation for a specific SBOM vulnerability",
      description = "Updates a vulnerability analysis annotation for a specific SBOM vulnerability",
      responses = {
        @ApiResponse(responseCode = "200",
            description = "Vulnerability analysis annotation updated successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Target vulnerability not found")
      })

  @PUT
  @Path(SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.UPDATE_SBOM_VULNERABILITY_ANALYSIS)
  public VulnerabilityAnalysis saveVulnerabilityAnalysis(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "The version for a specific SBOM where the vulnerability " +
          "is present", required = true) @PathParam("version") String sbomVersion,
      @Parameter(description = "The vulnerability id of a vulnerability",
          required = true) @PathParam("refId") String refId,
      @RequestBody(description = "Vulnerability analysis details with component information",
          required = true) ApiSbomVulnerabilityAnalysisRequestDTO apiSbomVulnerabilityAnalysisRequestDto)
  {
    AuditData.get().setVulnerability(apiSbomVulnerabilityAnalysisRequestDto, refId);
    return apiSbomVulnerabilityService.saveVulnerabilityAnalysis(applicationId, sbomVersion, refId,
        apiSbomVulnerabilityAnalysisRequestDto);
  }

  @Operation(summary = "Deletes a Vulnerability analysis for a given component.",
      description = "Deletes a Vulnerability analysis for a given component.",
      responses = {
        @ApiResponse(responseCode = "404", description = "Vulnerability analysis not found"),
        @ApiResponse(responseCode = "204", description = "Vulnerability analysis deleted")
      })
  @DELETE
  @Path(SBOM_VULNERABILITY_ANALYSIS_ANNOTATION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Consumes({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.DELETE_SBOM_VULNERABILITY_ANALYSIS)
  public Response deleteVulnerabilityAnalysis(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,
      @Parameter(description = "The version for a specific SBOM where the vulnerability " +
          "is present", required = true) @PathParam("version") String sbomVersion,
      @Parameter(description = "The vulnerability id of a vulnerability",
          required = true) @PathParam("refId") String refId,
      @RequestBody(description = "Hash or packageUrl to identify the component",
          required = true) ComponentLocator componentLocator)
  {
    AuditData.get().setVulnerability(componentLocator, refId);
    return apiSbomVulnerabilityService.deleteVulnerabilityAnalysis(applicationId, sbomVersion, refId,
        componentLocator);
  }
}
