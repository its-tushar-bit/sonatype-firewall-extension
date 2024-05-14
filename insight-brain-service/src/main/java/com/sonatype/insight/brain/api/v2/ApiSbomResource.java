/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiSbomStatusDTO;
import com.sonatype.insight.brain.api.v2.service.ApiSbomService;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataSummaryListDTO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.thirdpartyscans.SbomComponentDTO;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.SBOM_RESOURCE_PATH)
public class ApiSbomResource
{
  static final String DEFAULT_SBOM_STATE = "current";

  static final String DEFAULT_SBOM_SPECIFICATION = "cyclonedx1.5";

  public static final String SBOMS_APPLICATIONS_PATH = "/applications";

  static final String SBOMS_APPLICATION_PATH = SBOMS_APPLICATIONS_PATH + "/{applicationId}";

  static final String SBOM_VERSIONS_PATH = SBOMS_APPLICATION_PATH + "/versions";

  public static final String SBOM_VERSION_PATH = SBOM_VERSIONS_PATH + "/{version}";

  static final String SBOM_COMPONENTS_PATH = SBOM_VERSION_PATH + "/components";

  static final String SBOM_IMPORT_PATH = "/import";

  public static final String SBOM_STATUS_PATH = SBOMS_APPLICATION_PATH + "/status/{importRequestId}";

  private final ApiSbomService apiSbomService;

  @Inject
  public ApiSbomResource(final ApiSbomService apiSbomService) {
    this.apiSbomService = apiSbomService;
  }

  @Operation(summary = "Delete sbom version",
      tags = {"sbom"},
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
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "URL Encoded version value of the sbom to be deleted", required = true)
      @PathParam("version") String version) throws IOException
  {
    apiSbomService.deleteSbomVersion(applicationId, version);
  }

  @Operation(summary = "Gets a sbom version",
      tags = {"sbom"},
      description = "Downloads a specific sbom version in its original or current form",
      responses = {
          @ApiResponse(responseCode = "404", description = "Supplied sbom version not found"),
          @ApiResponse(responseCode = "200",
              description = "Content of the sbom",
              content = @Content(mediaType = "application/json|application/xml"))
      })

  @GET
  @Path(SBOM_VERSION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response getSbomVersion(
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId,

      @Parameter(description = "URL Encoded version value of the sbom", required = true)
      @PathParam("version") String version,

      @Parameter(description = "The state of the sbom version. Allowed values [original|current]. default = current")
      @DefaultValue(DEFAULT_SBOM_STATE) @QueryParam("state") String sbomState,

      @Parameter(description = "Target specification of the sbom. Allowed values [cyclonedx1.5|spdx2.3]. " +
          "default = cyclonedx1.5")
      @DefaultValue(DEFAULT_SBOM_SPECIFICATION) @QueryParam("specification") String targetSpecification,

      @Parameter(in = ParameterIn.HEADER, name = "Accept", description = "Output format(json/xml) of the sbom. " +
          "Changing the output format only applicable when downloading the current form of the SBOM. " +
          "The original sbom will always return in the original form that it was ingested. " +
          "When requesting `current` form and if this header value is not present the sbom will be returned " +
          "in its original ingested format. " +
          "Allowed values {'application/json'|'application/xml'}. default = null")
      @HeaderParam(HttpHeaders.ACCEPT) String acceptMediaType)
  {
    return apiSbomService.getSbomVersion(applicationId, version, sbomState, targetSpecification, acceptMediaType);
  }

  @Operation(summary = "Gets a paginated list of SBOMs for an application",
      tags = {"sbom"},
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
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId,

      @Parameter(description = "Sort results by import date. Allowed values [asc|desc]. default = asc")
      @DefaultValue("asc") @QueryParam("sortByDate") String sortByDate,

      @Parameter(description = "Number of items to return by page. default = 10")
      @DefaultValue("10") @QueryParam("pageSize") int pageSize,

      @Parameter(description = "Current page number. default = 1")
      @DefaultValue("1") @QueryParam("page") int page)
  {
    return apiSbomService.getSbomMetadataSummaryForApplication(applicationId, sortByDate, pageSize, page);
  }

  @Operation(summary = "Gets the components found in a specific sbom version", tags = {"sbom"},
      description = "Lists the components in a specific sbom version with data about vulnerabilities and licenses",
      responses = {
          @ApiResponse(responseCode = "200", description = "List of components in the sbom",
              content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOM_COMPONENTS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces(MediaType.APPLICATION_JSON)
  public List<SbomComponentDTO> getSbomComponents(
      @Parameter(description = "The internal id of the application",
          required = true) @PathParam("applicationId") String applicationId,

      @Parameter(description = "URL Encoded version value of the sbom to query its components",
          required = true) @PathParam("version") String version)
  {
    return apiSbomService.getSbomComponents(applicationId, version);
  }

  @Operation(summary = "Gets a list of sbom versions by application id",
      tags = {"sbom"},
      description = "Gets a list of sbom versions by application id",
      responses = {
          @ApiResponse(responseCode = "200",
              description = "list of the sbom versions by application id",
              content = @Content(mediaType = "application/json"))
      })

  @GET
  @Path(SBOM_VERSIONS_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  public List<String> getSbomVersionListByApplication(
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId
  )
  {
    return apiSbomService.getSbomVersionListByApplication(applicationId);
  }

  @Operation(summary = "Import a new sbom version",
      tags = {"sbom"},
      description = "Imports a new sbom version to an existing application",
      responses = {
          @ApiResponse(responseCode = "400", description = "Invalid/Unsupported data provided for sbom import"),
          @ApiResponse(responseCode = "202",
              description = "Import successful. URL to check the status of the import returned",
              content = @Content(mediaType = "application/json"))
      })

  @POST
  @Path(SBOM_IMPORT_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Produces({MediaType.APPLICATION_JSON})
  @Audited(AuditEvent.IMPORT_SBOM_VERSION)
  public Response importSbom(
      @Parameter(description = "The internal id of the application", required = true)
      @FormDataParam("applicationId") String applicationId,
      @FormDataParam("file") InputStream inputStream,
      @Context final HttpServletRequest request
  )
  {
    if (StringUtils.isBlank(applicationId)) {
      throw new BadRequestException("Missing required parameter [applicationId]");
    }

    return apiSbomService.importSbom(applicationId, inputStream, HdsClient.getClientUserAgent(request));
  }

  @Operation(summary = "Get sbom import status",
      tags = {"sbom"},
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
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "The id of the import request", required = true)
      @PathParam("importRequestId") String importRequestId
  )
  {
    return apiSbomService.getImportStatus(applicationId, importRequestId);
  }
}
