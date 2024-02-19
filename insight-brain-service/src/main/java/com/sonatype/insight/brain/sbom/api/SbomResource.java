/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.api;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
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
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.product.license.ProductLicenseEnforcementPoint;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Named
@Timed
@Singleton
@Path(PublicApiPaths.SBOM_RESOURCE_PATH)
public class SbomResource
{
  static final String DEFAULT_SBOM_FORM = "current";

  static final String SBOM_APPLICATION_PATH = "{applicationId}";

  static final String SBOM_VERSIONS_PATH = SBOM_APPLICATION_PATH + "/version";

  static final String SBOM_VERSION_PATH = SBOM_VERSIONS_PATH + "/{sbomVersion}";

  @Operation(summary = "Delete sbom version",
      tags = {"sbom"},
      description = "Deletes a specific sbom version including it's original contents and updates",
      responses = {
          @ApiResponse(responseCode = "404", description = "Supplied application id or the sbomVersion not found"),
          @ApiResponse(responseCode = "400",
              description = "An invalid sbomVersion value. Must be a positive integer >= 1"),
          @ApiResponse(responseCode = "204", description = "Delete successful")
      })

  @DELETE
  @Path(SBOM_VERSION_PATH)
  @ProductLicenseEnforcementPoint(LicensedFeature.SBOM_MANAGER)
  @Audited(AuditEvent.DELETE_SBOM_VERSION)
  public Response deleteSbomVersion(
      @Parameter(description = "The internal id of the application", required = true)
      @PathParam("applicationId") String applicationId,
      @Parameter(description = "sbomVersion value of the sbom to be deleted",
          schema = @Schema(type = "integer"), required = true)
      @PathParam("sbomVersion") Integer version)
  {
    //TODO: implement with CLM-29445
    return Response.noContent().build();
  }

  @Operation(summary = "Gets a sbom version",
      tags = {"sbom"},
      description = "Downloads a specific sbom version in its original or current form",
      responses = {
          @ApiResponse(responseCode = "404", description = "Supplied application id or the sbomVersion not found"),
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

      @Parameter(description = "sbomVersion value of the sbom to be deleted",
          schema = @Schema(type = "integer"), required = true)
      @PathParam("sbomVersion") Integer version,

      @Parameter(description = "The form of the sbom version. Allowed values [original|current]. default = current")
      @DefaultValue(DEFAULT_SBOM_FORM) @QueryParam("form") String sbomForm,

      @Parameter(in = ParameterIn.HEADER, name = "Accept", description = "Output format(json/xml) of the sbom. " +
          "Changing the output format only applicable when downloading the current form of the SBOM. " +
          "The original sbom will always return in the original form that it was ingested. " +
          "When requesting `current` form and if this header value is not present the sbom will be returned " +
          "in its original ingested format. " +
          "Allowed values {'application/json'|'application/xml'}. default = null")
      @HeaderParam(HttpHeaders.ACCEPT) String acceptMediaType)
  {
    //TODO: with CLM-29415
    return Response.ok().build();
  }
}
